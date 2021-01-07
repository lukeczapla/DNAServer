package edu.dnatools.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Protein.class)
public abstract class Protein_ {

	public static volatile SingularAttribute<Protein, Byte> number;
	public static volatile SingularAttribute<Protein, Short> stepLength;
	public static volatile SingularAttribute<Protein, String> pdbs;
	public static volatile SingularAttribute<Protein, String> dats;
	public static volatile SingularAttribute<Protein, String> name;
	public static volatile SingularAttribute<Protein, Long> id;

	public static final String NUMBER = "number";
	public static final String STEP_LENGTH = "stepLength";
	public static final String PDBS = "pdbs";
	public static final String DATS = "dats";
	public static final String NAME = "name";
	public static final String ID = "id";

}

