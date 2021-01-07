package edu.dnatools.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Job.class)
public abstract class Job_ {

	public static volatile SingularAttribute<Job, Long> proteinId;
	public static volatile SingularAttribute<Job, Long> seed;
	public static volatile SingularAttribute<Job, String> fixedPositions;
	public static volatile SingularAttribute<Job, Double> gBounds;
	public static volatile SingularAttribute<Job, String> description;
	public static volatile SingularAttribute<Job, Integer> nChains;
	public static volatile SingularAttribute<Job, Boolean> hasFixedProteins;
	public static volatile SingularAttribute<Job, String> token;
	public static volatile SingularAttribute<Job, Boolean> hasProteins;
	public static volatile SingularAttribute<Job, String> sequence;
	public static volatile SingularAttribute<Job, Double> bindingProbability;
	public static volatile SingularAttribute<Job, String> tp0;
	public static volatile SingularAttribute<Job, Byte> nProteins;
	public static volatile SingularAttribute<Job, String> bounds;
	public static volatile SingularAttribute<Job, Double> twBounds;
	public static volatile SingularAttribute<Job, Long> id;
	public static volatile SingularAttribute<Job, User> user;
	public static volatile SingularAttribute<Job, String> FC;
	public static volatile SingularAttribute<Job, String> fixedProteins;
	public static volatile SingularAttribute<Job, Double> rBounds;

	public static final String PROTEIN_ID = "proteinId";
	public static final String SEED = "seed";
	public static final String FIXED_POSITIONS = "fixedPositions";
	public static final String G_BOUNDS = "gBounds";
	public static final String DESCRIPTION = "description";
	public static final String N_CHAINS = "nChains";
	public static final String HAS_FIXED_PROTEINS = "hasFixedProteins";
	public static final String TOKEN = "token";
	public static final String HAS_PROTEINS = "hasProteins";
	public static final String SEQUENCE = "sequence";
	public static final String BINDING_PROBABILITY = "bindingProbability";
	public static final String TP0 = "tp0";
	public static final String N_PROTEINS = "nProteins";
	public static final String BOUNDS = "bounds";
	public static final String TW_BOUNDS = "twBounds";
	public static final String ID = "id";
	public static final String USER = "user";
	public static final String F_C = "FC";
	public static final String FIXED_PROTEINS = "fixedProteins";
	public static final String R_BOUNDS = "rBounds";

}

