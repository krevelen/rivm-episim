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
package nl.rivm.cib.episim.time;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.measure.quantity.Dimensionless;

//import javax.measure.quantity.Dimensionless;
//import javax.measure.quantity.Duration;

import org.jscience.physics.amount.Amount;

import io.coala.exception.ExceptionFactory;
import io.coala.time.x.Duration;
import io.coala.time.x.Instant;
import io.coala.time.x.TimeSpan;
import nl.rivm.cib.episim.util.Caller;
import rx.Observable;

/**
 * {@link Timed}
 * 
 * {@link List#stream()}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface Timed
{

	/**
	 * @return the {@link Scheduler} of this {@link Timed} object
	 */
	//default 
	Scheduler scheduler();
//	{
//		return SCHEDULER_INSTANCE;
//	}

	/** @return the current {@link Instant} */
	default Instant now()
	{
		return scheduler().now();
	}

	/**
	 * @param delay the future {@link Instant}
	 * @return the {@link FutureSelf}
	 */
	default FutureSelf at( final Instant when )
	{
		return FutureSelf.of( this, when );
	}

//	default FutureSelf at( final Amount<?> when )
//	{
//		return at( Instant.of( when ) );
//	}
//
//	default FutureSelf at( final Measure<?, ?> when )
//	{
//		return at( Instant.of( when ) );
//	}
//
//	default FutureSelf at( final ReadableInstant when )
//	{
//		return at( Instant.of( when ) );
//	}
//
//	default FutureSelf at( final Number when )
//	{
//		return at( Instant.of( when ) );
//	}

	default Observable<FutureSelf> atEach( final Observable<Instant> stream )
	{
		return stream.map( ( Instant t ) ->
		{
			return FutureSelf.of( this, t );
		} );
	}

	/**
	 * @param delay the {@link Amount} of delay, in ({@link Duration} or
	 *            {@link Dimensionless} units
	 * @return the {@link FutureSelf}
	 */
	default FutureSelf after( final Duration delay )
	{
		return FutureSelf.of( this, now().add( delay ) );
	}

	/**
	 * @param delay the {@link Amount} of delay, in ({@link Duration} or
	 *            {@link Dimensionless} units
	 * @return the {@link FutureSelf}
	 */
	default FutureSelf after( final TimeSpan delay )
	{
		return FutureSelf.of( this, now().add( delay ) );
	}

	/**
	 * {@link FutureSelf} is a decorator of a {@link Timed} object that is
	 * itself {@link Timed} but with its {@link #now()} at a fixed (future)
	 * {@link Instant} and additional scheduling helper methods
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface FutureSelf extends Timed
	{
		Timed self();

		@Override
		default Scheduler scheduler()
		{
			return self().scheduler();
		}

		/**
		 * @param call the (checked) {@link Callable} method to schedule
		 * @return the {@link Expectation} of the scheduled call
		 */
		default Expectation call( final Callable<?> call )
		{
			return scheduler().schedule( now(), call );
		}

		default Expectation call( final Runnable r )
		{
			return call( Caller.of( r ) );
		}

		default <T> Expectation call( final Consumer<T> c, final T t )
		{
			return call( Caller.of( c, t ) );
		}

		default <T, U> Expectation call( final BiConsumer<T, U> c, final T t,
			final U u )
		{
			return call( Caller.of( c, t, u ) );
		}

		default <T, R> Expectation call( final Function<T, R> c, final T t )
		{
			return call( Caller.of( c, t ) );
		}

		default <T, U, R> Expectation call( final BiFunction<T, U, R> c,
			final T t, final U u )
		{
			return call( Caller.of( c, t, u ) );
		}

		static FutureSelf of( final Timed self, final Instant when )
		{
			if( self.now().compareTo( when ) > 0 ) throw ExceptionFactory
					.createUnchecked( "Can't schedule in past: {} < now({})",
							when, self.now() );
			return new FutureSelf()
			{
				@Override
				public Timed self()
				{
					return self;
				}

				@Override
				public Instant now()
				{
					return when;
				}
			};
		}
	}
}