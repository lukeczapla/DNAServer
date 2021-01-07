package edu.dnatools.model;

import java.util.Date;
import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(RCSBAnalysis.class)
public abstract class RCSBAnalysis_ {

	public static volatile SingularAttribute<RCSBAnalysis, String> code;
	public static volatile SingularAttribute<RCSBAnalysis, Date> afterDate;
	public static volatile SingularAttribute<RCSBAnalysis, Boolean> RNA;
	public static volatile SingularAttribute<RCSBAnalysis, String> description;
	public static volatile SingularAttribute<RCSBAnalysis, Long> id;
	public static volatile SingularAttribute<RCSBAnalysis, String> pdbList;
	public static volatile SingularAttribute<RCSBAnalysis, User> user;
	public static volatile SingularAttribute<RCSBAnalysis, Date> beforeDate;
	public static volatile SingularAttribute<RCSBAnalysis, Boolean> done;
	public static volatile SingularAttribute<RCSBAnalysis, Boolean> redundant;

	public static final String CODE = "code";
	public static final String AFTER_DATE = "afterDate";
	public static final String R_NA = "RNA";
	public static final String DESCRIPTION = "description";
	public static final String ID = "id";
	public static final String PDB_LIST = "pdbList";
	public static final String USER = "user";
	public static final String BEFORE_DATE = "beforeDate";
	public static final String DONE = "done";
	public static final String REDUNDANT = "redundant";

}

