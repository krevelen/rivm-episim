/* $Id$
 * 
 * Part of ZonMW project no. 50-53000-98-156
 * 
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Copyright (c) 2016 RIVM National Institute for Health and Environment 
 */
package nl.rivm.cib.demo;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.aeonbits.owner.ConfigFactory;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.ujmp.core.Matrix;
import org.ujmp.core.enums.ValueType;

import io.coala.bind.InjectConfig;
import io.coala.bind.LocalBinder;
import io.coala.bind.LocalConfig;
import io.coala.data.DataLayer;
import io.coala.data.Table;
import io.coala.dsol3.Dsol3Scheduler;
import io.coala.log.LogUtil;
import io.coala.math3.Math3ProbabilityDistribution;
import io.coala.math3.Math3PseudoRandom;
import io.coala.random.DistributionParser;
import io.coala.random.ProbabilityDistribution;
import io.coala.random.PseudoRandom;
import io.coala.time.Scenario;
import io.coala.time.Scheduler;
import io.coala.time.SchedulerConfig;
import io.coala.util.MapBuilder;
import nl.rivm.cib.demo.DemoModel.Demical.DemicalEvent;
import nl.rivm.cib.demo.DemoModel.Demical.SimpleDeme;
import nl.rivm.cib.demo.DemoModel.Households.HouseholdTuple;
import nl.rivm.cib.demo.DemoModel.Persons.PersonTuple;
import nl.rivm.cib.episim.cbs.TimeUtil;

/**
 * {@link BrokerTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class BrokerTest
{
	/** */
	private static final Logger LOG = LogUtil.getLogger( BrokerTest.class );

//	public interface Manager<ID, T extends Tuple>
//	{
//		DataAccessor<ID, T> data();
//
//		class Simple<ID, T extends Tuple> implements Manager<ID, T>
//		{
//			private final DataAccessor<ID, T> data;
//
//			public Simple()
//			{
//				this( new DataAccessor.Factory.MapAccessor() );
//			}
//
//			@SuppressWarnings( "unchecked" )
//			@Inject
//			public Simple( final DataAccessor.Factory factory )
//			{
//				final List<Class<?>> typeArgs = TypeArguments.of( Manager.class,
//						getClass() );
//				final Class<ID> keyType = (Class<ID>) typeArgs.get( 0 );
//				final Class<T> tupleType = (Class<T>) typeArgs.get( 1 );
//				this.data = factory.create( keyType, tupleType );
//			}
//
//			@Override
//			public DataAccessor<ID, T> data()
//			{
//				return this.data;
//			}
//		}
//	}

	public static class DemoScenario implements DemoModel, Scenario
	{
		@InjectConfig
		private DemoConfig config;

		@Inject
		private LocalBinder binder;

		@Inject
		private DataLayer data;

		@Inject
		private Scheduler scheduler;

		// the data sources
		private Matrix persons, households;

		// model event statistics
		private final Map<Class<?>, AtomicLong> demeEventStats = new HashMap<>();

		// data event statistics
		private final Map<Class<?>, AtomicLong> dataEventStats = new HashMap<>();

		@Override
		public Scheduler scheduler()
		{
			return this.scheduler;
		}

		@Override
		public void init() throws Exception
		{
			LOG.trace( "cfg: {}", this.config );

			this.persons = Matrix.Factory.sparse( ValueType.OBJECT, 20_000_000,
					Persons.PROPERTIES.size() );
			this.households = Matrix.Factory.sparse( ValueType.OBJECT,
					10_000_000, Households.PROPERTIES.size() );

			// register data sources
			this.data
					.withSource( this.persons,
							MapBuilder.<Class<?>, List<Class<?>>>unordered()
									.put( PersonTuple.class,
											Persons.PROPERTIES )
									.build() )
					.withSource( this.households, MapBuilder
							.<Class<?>, List<Class<?>>>unordered()
							.put( HouseholdTuple.class, Households.PROPERTIES )
							.build() );

			// maintain data event statistics
			this.data.changes().filter( chg -> chg.crud() != Table.CRUD.UPDATE )
					.subscribe( chg -> this.dataEventStats
							.computeIfAbsent( chg.changedType(),
									k -> new AtomicLong() )
							.addAndGet(
									chg.crud() == Table.CRUD.CREATE ? 1 : -1 ),
							scheduler()::fail );

			// populate and run deme
			final AtomicLong t0 = new AtomicLong();
			this.binder
					.inject( SimpleDeme.class,
							this.config.toJSON( DemoConfig.SCENARIO_BASE,
									DemoConfig.POPULATION_BASE ) )
					.reset().events()//.observeOn( Schedulers.io() )
					.ofType( DemicalEvent.class ).subscribe( ev ->
					{
						t0.updateAndGet( ms ->
						{
							final long t = System.currentTimeMillis();
							if( t - ms < 1000 ) return ms;
							logStats();
							return t;
						} );
						this.demeEventStats
								.computeIfAbsent( ev.getClass(),
										k -> new AtomicLong() )
								.incrementAndGet();
					}, scheduler()::fail, this::logStats );
		}

		private void logStats()
		{
			LOG.trace( "t={} data: {}, deme event x {}: {}",
					scheduler().now( DateTimeFormatter.ISO_WEEK_DATE ),
					this.dataEventStats.entrySet().stream()
							.map( e -> e.getKey().getSimpleName() + " x "
									+ e.getValue() )
							.reduce( ( s1, s2 ) -> String.join( ", ", s1, s2 ) )
							.orElse( "[]" ),
					this.demeEventStats.values().stream()
							.mapToLong( AtomicLong::get ).sum(),
					String.join( ", ",
							this.demeEventStats.entrySet().stream()
									.map( e -> e.getKey().getSimpleName() + "="
											+ e.getValue() )
									.toArray( String[]::new ) ) );
		}
	}

	@Test
	public void doTest() throws InterruptedException
	{
		LOG.info( "Test started" );

		final DemoConfig config = ConfigFactory.create( DemoConfig.class );
		final ZonedDateTime offset = config.offset()
				.atStartOfDay( TimeUtil.NL_TZ );
		final long durationDays = Duration
				.between( offset, offset.plus( config.duration() ) ).toDays();
		final LocalConfig binderConfig = LocalConfig.builder().withProvider(
				Scheduler.class, Dsol3Scheduler.class,
				MapBuilder.unordered()
						.put( SchedulerConfig.ID_KEY, "" + config.setupName() )
						.put( SchedulerConfig.OFFSET_KEY, "" + offset )
						.put( SchedulerConfig.DURATION_KEY, "" + durationDays )
						.build() )
				.withProvider( DataLayer.class, DataLayer.Simple.class )
				.withProvider( Scenario.class, DemoScenario.class )
				.withProvider( ProbabilityDistribution.Parser.class,
						DistributionParser.class )
				.build();

		// FIXME workaround until seed becomes configurable in coala
		final LocalBinder binder = binderConfig
				.createBinder( MapBuilder.<Class<?>, Object>unordered()
						.put( ProbabilityDistribution.Factory.class,
								new Math3ProbabilityDistribution.Factory(
										new Math3PseudoRandom.MersenneTwisterFactory()
												.create( PseudoRandom.Config.NAME_DEFAULT,
														config.randomSeed() ) ) )
						.build() );

		final Scenario model = binder.inject( Scenario.class );
		model.run();

		LOG.info( "Test done" );
	}
}