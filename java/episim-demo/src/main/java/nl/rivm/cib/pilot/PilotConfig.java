package nl.rivm.cib.pilot;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URI;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.measure.Quantity;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Time;

import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.Converter;

import io.coala.bind.LocalBinder;
import io.coala.config.ConfigUtil;
import io.coala.config.GlobalConfig;
import io.coala.config.LocalDateConverter;
import io.coala.config.PeriodConverter;
import io.coala.exception.Thrower;
import io.coala.json.JsonUtil;
import io.coala.math.DecimalUtil;
import io.coala.math.QuantityConfigConverter;
import io.coala.math.QuantityUtil;
import io.coala.math.Range;
import io.coala.math.WeightedValue;
import io.coala.persist.JPAConfig;
import io.coala.random.ConditionalDistribution;
import io.coala.random.ProbabilityDistribution;
import io.coala.random.ProbabilityDistribution.Factory;
import io.coala.random.ProbabilityDistribution.Parser;
import io.coala.random.PseudoRandom;
import io.coala.random.QuantityDistribution;
import io.coala.time.Instant;
import io.coala.time.Scheduler;
import io.coala.time.TimeUnits;
import io.coala.time.Timing;
import io.coala.util.InputStreamConverter;
import io.coala.util.MapBuilder;
import io.reactivex.observables.GroupedObservable;
import nl.rivm.cib.epidemes.cbs.json.CBSHousehold;
import nl.rivm.cib.epidemes.cbs.json.CBSRegionType;
import nl.rivm.cib.epidemes.cbs.json.Cbs83287Json;
import nl.rivm.cib.epidemes.cbs.json.CbsNeighborhood;
import nl.rivm.cib.episim.model.SocialGatherer;
import nl.rivm.cib.episim.model.locate.Region;
import nl.rivm.cib.episim.model.locate.Region.ID;
import nl.rivm.cib.episim.model.vaccine.attitude.VaxOccasion;
import nl.rivm.cib.json.HesitancyProfileJson;
import nl.rivm.cib.json.RelationFrequencyJson;
import nl.rivm.cib.json.HesitancyProfileJson.HesitancyDimension;
import nl.rivm.cib.pilot.hh.HHAttitudeEvaluator;
import nl.rivm.cib.pilot.hh.HHAttitudePropagator;
import nl.rivm.cib.pilot.hh.HHAttractor;
import nl.rivm.cib.pilot.hh.HHAttribute;
import tec.uom.se.ComparableQuantity;

/**
 * {@link PilotConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Sources( {
		"file:${" + PilotConfig.CONFIG_BASE_KEY + "}"
				+ PilotConfig.CONFIG_YAML_FILE,
		"file:" + PilotConfig.CONFIG_BASE_DIR + PilotConfig.CONFIG_YAML_FILE,
		"classpath:" + PilotConfig.CONFIG_YAML_FILE } )
public interface PilotConfig extends GlobalConfig
{

	/** configuration file name system property */
	String CONFIG_BASE_KEY = "config.base";

	/** configuration and data file base directory */
	String CONFIG_BASE_DIR = "dist/";

	String CONFIG_YAML_FILE = "sim.yaml";

	String DATASOURCE_JNDI = "jdbc/pilotDB";

	/** configuration key separator */
	String KEY_SEP = ConfigUtil.CONFIG_KEY_SEP;

	/** configuration key */
	String SCENARIO_BASE = "scenario";

	/** configuration key */
	String REPLICATION_PREFIX = SCENARIO_BASE + KEY_SEP + "replication"
			+ KEY_SEP;

	/** configuration key */
	String STATISTICS_PREFIX = REPLICATION_PREFIX + "statistics" + KEY_SEP;

	/** configuration key */
	String POPULATION_PREFIX = SCENARIO_BASE + KEY_SEP + "demography" + KEY_SEP;

	/** configuration key */
	String MOBILITY_PREFIX = SCENARIO_BASE + KEY_SEP + "mobility" + KEY_SEP;

	/** configuration key */
	String HESITANCY_PREFIX = SCENARIO_BASE + KEY_SEP + "hesitancy" + KEY_SEP;

	/** configuration key */
	String GEOGRAPHY_PREFIX = SCENARIO_BASE + KEY_SEP + "geography" + KEY_SEP;

	/** configuration key */
	String VACCINATION_PREFIX = SCENARIO_BASE + KEY_SEP + "vaccination"
			+ KEY_SEP;

	/** configuration key */
	String PATHOLOGY_PREFIX = SCENARIO_BASE + KEY_SEP + "pathology" + KEY_SEP;

	@DefaultValue( CONFIG_BASE_DIR )
	@Key( CONFIG_BASE_KEY )
	String configBase();

	// match unit name from persistence.xml
	@DefaultValue( "" + false )
	@Key( STATISTICS_PREFIX + "db-enabled" )
	boolean dbEnabled();

	// match unit name from persistence.xml
	@DefaultValue( "hh_pu" )
	@Key( JPAConfig.JPA_UNIT_NAMES_KEY )
	@Separator( JPAConfig.NAME_DELIMITER )
	String[] jpaPersistenceUnitNames();

	// jdbc:neo4j:bolt://192.168.99.100:7687/db/data
	// jdbc:mysql://localhost/hhdb
	// jdbc:h2:~/morphdat/h2_hhdb
	// jdbc:h2:tcp://localhost/~/morphdat/h2_hhdb
	@DefaultValue( "jdbc:h2:tcp://localhost/~/epidemes/pilot_h2" )
	@Key( JPAConfig.JPA_JDBC_URL_KEY )
	URI jdbcUrl();

	@DefaultValue( "sa" )
	@Key( JPAConfig.JPA_JDBC_PASSWORD_KEY )
	String dbcUser();

	@DefaultValue( "sa" )
	@Key( JPAConfig.JPA_JDBC_USER_KEY )
	String jdbcPassword();

	default <T extends JPAConfig> T toJPAConfig( final Class<T> jpaConfigType,
		final Map<?, ?>... imports )
	{

		// bind a local HSQL data source for exporting statistics
//		JndiUtil.bindLocally( HHConfig.DATASOURCE_JNDI, '/', () ->
//		{
//			final JDBCDataSource ds = new JDBCDataSource();
//			ds.setUrl( hhConfig.hsqlUrl() );
//			ds.setUser( hhConfig.hsqlUser() );
//			ds.setPassword( hhConfig.hsqlPassword() );
//			return ds;
//		} );

		return ConfigFactory.create( jpaConfigType,
				ConfigUtil.join( export( Pattern.compile(
						"^(" + Pattern.quote( "javax.persistence" ) + ").*" ) ),
						imports ) );
	}

	/////////////////////////////////////////////////////////////////////////

	/**
	 * {@link RandomSeedConverter} defaults to
	 * {@link System#currentTimeMillis()} + {@link System#nanoTime()}
	 */
	class RandomSeedConverter implements Converter<Long>
	{
		@Override
		public Long convert( final Method method, final String input )
		{
			try
			{
				return Long.valueOf( input );
			} catch( final NumberFormatException e )
			{
				return System.currentTimeMillis() & System.nanoTime();
			}
		}
	}

	@Key( REPLICATION_PREFIX + "setup-name" )
	@DefaultValue( "pilot" )
	String setupName();

	@Key( REPLICATION_PREFIX + "random-seed" )
	@DefaultValue( "NAN" )
	@ConverterClass( RandomSeedConverter.class )
	Long randomSeed();

	@Key( REPLICATION_PREFIX + "duration-period" )
	@DefaultValue( "P1Y" )
	@ConverterClass( PeriodConverter.class )
	Period duration();

	@Key( REPLICATION_PREFIX + "offset-date" )
	@DefaultValue( "2012-01-01" )
	@ConverterClass( LocalDateConverter.class )
	LocalDate offset();

	/**
	 * <a
	 * href=http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html>Cron
	 * trigger pattern</a> for timing population statistics export, e.g.
	 * <ol type=a>
	 * <li>{@code "0 0 0 ? * MON *"} : <em>midnight on every Monday</em></li>
	 * <li>{@code "0 0 0 1,15 * ? *"} : <em>midnight on every 1st and 15th day
	 * of the month</em></li>
	 * </ol>
	 */
	@Key( STATISTICS_PREFIX + "recurrence" )
	@DefaultValue( "0 0 0 1 * ? *" )
	String statisticsRecurrence();

	default Iterable<Instant> statisticsRecurrence( final Scheduler scheduler )
		throws ParseException
	{
		return Timing.of( statisticsRecurrence() ).iterate( scheduler );
	}

	@Key( POPULATION_PREFIX + "population-size" )
	@DefaultValue( "" + 1000 )
	long populationSize();

	@Key( POPULATION_PREFIX + "hh-type-dist" )
	@DefaultValue( "const(SOLO_1KID)" )
	String householdTypeDist();

	default ProbabilityDistribution<CBSHousehold>
		householdTypeDist( final Parser distParser ) throws ParseException
	{
		return distParser.parse( householdTypeDist(), CBSHousehold.class );
	}

	@Key( POPULATION_PREFIX + "hh-ref-age-dist" )
	@DefaultValue( "uniform(20;40)" )
	String householdReferentAgeDist();

	default QuantityDistribution<Time> householdReferentAgeDist(
		final Parser distParser ) throws ParseException
	{
		return distParser.<Number>parse( householdReferentAgeDist() )
				.toQuantities( TimeUnits.ANNUM );
	}

	@Key( POPULATION_PREFIX + "hh-ref-male-prob" )
	@DefaultValue( ".5" )
	double householdReferentMaleProb();

	default ProbabilityDistribution<Boolean> householdReferentMaleDist(
		final Factory distFactory ) throws ParseException
	{
		return distFactory.createBernoulli( householdReferentMaleProb() );
	}

	/** @see TimeUnits#ANNUM_LABEL */
	@Key( POPULATION_PREFIX + "hh-leavehome-age" )
	@DefaultValue( "20 yr" )
	@ConverterClass( QuantityConfigConverter.class )
	Quantity<Time> householdLeaveHomeAge();

	/** @see TimeUnits#ANNUAL_LABEL */
	@Key( POPULATION_PREFIX + "hh-migrate-rate" )
	@DefaultValue( ".02 annual" )
	@ConverterClass( QuantityConfigConverter.class )
	Quantity<Frequency> householdMigrationRate();

	default QuantityDistribution<Time> householdReplacementDist(
		final Factory distFactory, final long hhTotal )
	{
		final Quantity<?> rate = householdMigrationRate().multiply( hhTotal )
				.to( TimeUnits.DAILY );
		return distFactory
				.createExponential( // mean <- 1/rate
						DecimalUtil.divide( BigDecimal.ONE,
								DecimalUtil.valueOf( rate.getValue() ) ) )
				.toQuantities( TimeUnits.DAYS );
	}

	static BigDecimal inverse( final Number value )
	{
		return DecimalUtil.divide( BigDecimal.ONE,
				DecimalUtil.valueOf( value ) );
	}

//	@Key( POPULATION_PREFIX + "cbs-region-type" )
//	@DefaultValue( "COROP" )
//	CBSRegionType cbsRegionLevel();
//
//	@Key( POPULATION_PREFIX + "cbs-71486ned-data" )
//	@DefaultValue( CONFIG_BASE_DIR + "71486ned-TS-2010-2016.json" )
//	@ConverterClass( InputStreamConverter.class )
//	InputStream cbs71486Data();
//
//	default ConditionalDistribution<Cbs71486json.Category, LocalDate> cbs71486(
//		final Range<LocalDate> timeFilter,
//		final ProbabilityDistribution.Factory distFactory )
//	{
//		final CBSRegionType cbsRegionLevel = cbsRegionLevel();
//		final Map<LocalDate, Collection<WeightedValue<Cbs71486json.Category>>> map = Cbs71486json
//				.readAsync( this::cbs71486Data, timeFilter )
//				.filter( wv -> wv.getValue().regionType() == cbsRegionLevel )
//				.toMultimap( wv -> wv.getValue().offset(), wv -> wv,
//						TreeMap::new )
//				.blockingGet();
//		return ConditionalDistribution.of( distFactory::createCategorical,
//				map );
//	}

	@Key( GEOGRAPHY_PREFIX + "region-fallback-ref" )
	@DefaultValue( "GM0363" )
	Region.ID regionFallbackRef();

	@Key( GEOGRAPHY_PREFIX + "regional-resolution" )
	@DefaultValue( "MUNICIPAL" )
	CBSRegionType regionalResolution();

	@Key( GEOGRAPHY_PREFIX + "region-mapping-data" )
	@DefaultValue( "${" + CONFIG_BASE_KEY + "}83287NED.json" )
	@ConverterClass( InputStreamConverter.class )
	InputStream regionMappingData();

	default Map<String, Map<CBSRegionType, String>>
		regionsPerMunicipalityMapping() throws IOException
	{
		return Cbs83287Json.parse( regionMappingData() ).toMap();
	}

	default Map<CBSRegionType, Map<String, Set<String>>> regionsPerTypeMapping()
		throws IOException
	{
		return Cbs83287Json.parse( regionMappingData() ).toReverseMap();
	}

	@Key( GEOGRAPHY_PREFIX + "region-density-data" )
	@DefaultValue( "${" + CONFIG_BASE_KEY + "}pc6_buurt.json" )
	@ConverterClass( InputStreamConverter.class )
	InputStream regionDensityData();

	default Map<ID, Collection<WeightedValue<CbsNeighborhood>>> regionDensity()
	{
		final CBSRegionType cbsRegionLevel = regionalResolution();
		return CbsNeighborhood.readAsync( this::regionDensityData )
				.toMultimap( bu -> bu.regionRef( cbsRegionLevel ),
						CbsNeighborhood::toWeightedValue )
				.blockingGet();
	}

	default ConditionalDistribution<CbsNeighborhood, Region.ID>
		regionDensityDist( final Factory distFactory,
			//final Map<String, Map<CBSRegionType, String>> regionTypes,
			final Function<Region.ID, Region.ID> fallback )
	{
		final CBSRegionType cbsRegionLevel = regionalResolution();
		final Map<Region.ID, ProbabilityDistribution<CbsNeighborhood>> async = CbsNeighborhood
				.readAsync( this::regionDensityData )
				.groupBy( bu -> bu.regionRef( cbsRegionLevel ) )
				.toMap( GroupedObservable::getKey,
						group -> distFactory.createCategorical( group
								.map( CbsNeighborhood::toWeightedValue )
								.toList( HashSet::new ).blockingGet() ) )
				.blockingGet();

		return ConditionalDistribution.of( id -> async.computeIfAbsent( id,
				key -> async.get( fallback.apply( key ) ) ) );
	}

	@SuppressWarnings( "rawtypes" )
	@Key( MOBILITY_PREFIX + "motor-factory" )
	@DefaultValue( "nl.rivm.cib.episim.model.SocialGatherer$Factory$SimpleBinding" )
	Class<? extends SocialGatherer.Factory> mobilityGathererFactory();

	@SuppressWarnings( "unchecked" )
	default NavigableMap<String, SocialGatherer>
		mobilityGatherers( final LocalBinder binder ) throws Exception
	{
		return Collections.unmodifiableNavigableMap(
				binder.inject( mobilityGathererFactory() )
						.createAll( toJSON( MOBILITY_PREFIX + "motors" ) ) );
	}

	@Key( VACCINATION_PREFIX + "invitation-age" )
	@DefaultValue( "[.5 yr; 4 yr)" )
	String vaccinationInvitationAge();

	@SuppressWarnings( "unchecked" )
	default Range<ComparableQuantity<Time>> vaccinationAgeRange()
		throws ParseException
	{
		return Range.parse( vaccinationInvitationAge(), QuantityUtil::valueOf )
				.map( v -> v.asType( Time.class ) );
	}

	@Key( VACCINATION_PREFIX + "occasion-recurrence" )
	@DefaultValue( "0 0 0 7 * ? *" )
	String vaccinationRecurrence();

	default Iterable<Instant> vaccinationRecurrence( final Scheduler scheduler )
		throws ParseException
	{
		return Timing.of( vaccinationRecurrence() ).iterate( scheduler );
	}

	/** @see VaxOccasion#utility() */
	@Key( VACCINATION_PREFIX + "occasion-utility-dist" )
	@DefaultValue( "const(0.5)" )
	String vaccinationUtilityDist();

	default ProbabilityDistribution<Number>
		vaccinationUtilityDist( final Parser distParser ) throws ParseException
	{
		return distParser.parse( vaccinationUtilityDist() );
	}

	/** @see VaxOccasion#proximity() */
	@Key( VACCINATION_PREFIX + "occasion-proximity-dist" )
	@DefaultValue( "const(0.5)" )
	String vaccinationProximityDist();

	default ProbabilityDistribution<Number> vaccinationProximityDist(
		final Parser distParser ) throws ParseException
	{
		return distParser.parse( vaccinationProximityDist() );
	}

	/** @see VaxOccasion#clarity() */
	@Key( VACCINATION_PREFIX + "occasion-clarity-dist" )
	@DefaultValue( "const(0.5)" )
	String vaccinationClarityDist();

	default ProbabilityDistribution<Number>
		vaccinationClarityDist( final Parser distParser ) throws ParseException
	{
		return distParser.parse( vaccinationClarityDist() );
	}

	/** @see VaxOccasion#affinity() */
	@Key( VACCINATION_PREFIX + "occasion-affinity-dist" )
	@DefaultValue( "const(0.5)" )
	String vaccinationAffinityDist();

	default ProbabilityDistribution<Number>
		vaccinationAffinityDist( final Parser distParser ) throws ParseException
	{
		return distParser.parse( vaccinationAffinityDist() );
	}

	@Key( HESITANCY_PREFIX + "attractor-factory" )
	@DefaultValue( "nl.rivm.cib.pilot.hh.HHAttractor$Factory$SimpleBinding" )
	Class<? extends HHAttractor.Factory> hesitancyAttractorFactory();

	default NavigableMap<String, HHAttractor>
		hesitancyAttractors( final LocalBinder binder )
	{
		try
		{
			return Collections.unmodifiableNavigableMap(
					binder.inject( hesitancyAttractorFactory() ).createAll(
							toJSON( HESITANCY_PREFIX + "attractors" ) ) );
		} catch( final Exception e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/** @see RelationFrequencyJson */
	@Key( HESITANCY_PREFIX + "relation-frequencies" )
	@DefaultValue( "${" + CONFIG_BASE_KEY + "}relation-frequency.json" )
	@ConverterClass( InputStreamConverter.class )
	InputStream hesitancyRelationFrequencies();

	default
		ConditionalDistribution<Quantity<Time>, RelationFrequencyJson.Category>
		hesitancyRelationFrequencyDist(
			final ProbabilityDistribution.Factory distFactory )
	{
		final List<RelationFrequencyJson> map = JsonUtil
				.readArrayAsync( () -> hesitancyRelationFrequencies(),
						RelationFrequencyJson.class )
				.toList().blockingGet();
		@SuppressWarnings( "unchecked" )
		final Quantity<Time> defaultDelay = QuantityUtil.valueOf( "1 yr" );
		return c ->
		{
			return map.stream()
					.filter( json -> json.relation == c.relation()
							&& json.gender == c.gender()
							&& json.ageRange().contains( c.floorAge() ) )
					.map( json -> json.intervalDist( distFactory ).draw() )
					.findFirst().orElse( defaultDelay );
		};
	}

	@Key( HESITANCY_PREFIX + "relation-impact-rate" )
	@DefaultValue( "1" )
	BigDecimal hesitancyRelationImpactRate();

	/** @see HesitancyProfileJson */
	@Key( HESITANCY_PREFIX + "profiles" )
	@DefaultValue( "${" + CONFIG_BASE_KEY + "}hesitancy-univariate.json" )
	@ConverterClass( InputStreamConverter.class )
	InputStream hesitancyProfiles();

	default ProbabilityDistribution<HesitancyProfileJson>
		hesitancyProfileDist( final Factory distFactory )
	{
		return distFactory.createCategorical( HesitancyProfileJson
				.parse( () -> hesitancyProfiles() ).toList().blockingGet() );
	}

	default <T> ConditionalDistribution<HesitancyProfileJson, T>
		hesitancyProfilesGrouped( final Factory distFactory,
			final Function<HesitancyProfileJson, T> keyMapper )
	{
		return ConditionalDistribution.of( distFactory::createCategorical,
				HesitancyProfileJson.parse( () -> hesitancyProfiles() )
						.toMultimap( wv -> keyMapper.apply( wv.getValue() ),
								wv -> wv )
						.blockingGet() );
	}

	@Key( HESITANCY_PREFIX + "profile-sample" )
	@DefaultValue( "${" + CONFIG_BASE_KEY + "}hesitancy-initial.json" )
	@ConverterClass( InputStreamConverter.class )
	InputStream hesitancyProfileSample();

	default
		ConditionalDistribution<Map<HHAttribute, BigDecimal>, HesitancyProfileJson>
		hesitancyProfileSample( final PseudoRandom rng ) throws IOException
	{
		final BigDecimal[][] sample = JsonUtil
				.valueOf( hesitancyProfileSample(), BigDecimal[][].class );
		final Map<HesitancyProfileJson, ProbabilityDistribution<Map<HHAttribute, BigDecimal>>> distCache = new HashMap<>();
		return ConditionalDistribution
				.of( hes -> distCache.computeIfAbsent( hes, key -> () ->
				{
					final int row = rng.nextInt( sample.length );
					final int confCol = hes.indices
							.get( HesitancyDimension.confidence ) - 1;
					final int compCol = hes.indices
							.get( HesitancyDimension.complacency ) - 1;
					return MapBuilder.<HHAttribute, BigDecimal>unordered()
							.put( HHAttribute.CONFIDENCE,
									DecimalUtil
											.valueOf( sample[row][confCol] ) )
							.put( HHAttribute.COMPLACENCY,
									DecimalUtil
											.valueOf( sample[row][compCol] ) )
							.build();
				} ) );
	}

	/**
	 * TODO from profile-data
	 * 
	 * @see HesitancyProfileJson
	 */
	@Key( HESITANCY_PREFIX + "calculation-dist" )
	@DefaultValue( "const(0.5)" )
	String hesitancyCalculationDist();

	default ProbabilityDistribution<BigDecimal> hesitancyCalculationDist(
		final Parser distParser ) throws ParseException
	{
		return distParser.<Number>parse( hesitancyCalculationDist() )
				.map( DecimalUtil::valueOf );
	}

	@Key( HESITANCY_PREFIX + "social-network-degree" )
	@DefaultValue( "10" )
	int hesitancySocialNetworkDegree();

	@Key( HESITANCY_PREFIX + "social-network-beta" )
	@DefaultValue( "0.5" ) // 0 = lattice, 1 = random network
	double hesitancySocialNetworkBeta();

	@Key( HESITANCY_PREFIX + "social-assortativity" )
	@DefaultValue( "0.75" )
	double hesitancySocialAssortativity();

	@Key( HESITANCY_PREFIX + "school-assortativity-dist" )
	@DefaultValue( "bernoulli(0.75)" )
	String hesitancySchoolAssortativity();

	default ProbabilityDistribution<Boolean> hesitancySchoolAssortativity(
		final Parser distParser ) throws ParseException
	{
		return distParser.parse( hesitancySchoolAssortativity() );
	}

	/** @see HHAttitudeEvaluator */
	@Key( HESITANCY_PREFIX + "evaluator" )
	@DefaultValue( "nl.rivm.cib.pilot.hh.HHAttitudeEvaluator$Average" )
	Class<? extends HHAttitudeEvaluator> attitudeEvaluatorType();

	default HHAttitudeEvaluator attitudeEvaluator()
		throws InstantiationException, IllegalAccessException
	{
		return attitudeEvaluatorType().newInstance();
	}

	/** @see HHAttitudePropagator */
	@Key( HESITANCY_PREFIX + "propagator" )
	@DefaultValue( "nl.rivm.cib.pilot.hh.HHAttitudePropagator$Shifted" )
	Class<? extends HHAttitudePropagator> attitudePropagatorType();

	default HHAttitudePropagator attitudePropagator()
		throws InstantiationException, IllegalAccessException
	{
		return attitudePropagatorType().newInstance();
	}

	/**
	 * <a
	 * href=http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html>Cron
	 * trigger pattern</a> for timing the attitude/hesitancy propagation, e.g.
	 * <ol type=a>
	 * <li>{@code "0 0 0 ? * MON *"} : <em>midnight on every Monday</em></li>
	 * <li>{@code "0 0 0 1,15 * ? *"} : <em>midnight on every 1st and 15th day
	 * of the month</em></li>
	 * </ol>
	 */
	@Key( HESITANCY_PREFIX + "propagator-recurrence" )
	@DefaultValue( "0 0 0 ? * MON *" )
	String attitudePropagatorRecurrence();

	default Iterable<Instant> attitudePropagatorRecurrence(
		final Scheduler scheduler ) throws ParseException
	{
		return Timing.of( attitudePropagatorRecurrence() ).iterate( scheduler );
	}

//	@Key( "morphine.measles.contact-period" )
//	@DefaultValue( "10 h" )
//	Duration contactPeriod();
//
//	@DefaultValue( "2 day" )
//	Duration latentPeriodConst();
//
//	@DefaultValue( "5 day" )
//	Duration recoverPeriodConst();
//
//	@DefaultValue( "9999 day" )
//	Duration wanePeriodConst();
//
//	@DefaultValue( "3 day" )
//	Duration onsetPeriodConst();
//
//	@DefaultValue( "7 day" )
//	Duration symptomPeriodConst();

}