package edu.dnatools.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(ProteinStructure.class)
public abstract class ProteinStructure_ {

	public static volatile SingularAttribute<ProteinStructure, String> name;
	public static volatile SingularAttribute<ProteinStructure, String> description;
	public static volatile SingularAttribute<ProteinStructure, Long> id;
	public static volatile SingularAttribute<ProteinStructure, String> PDBtext;

	public static final String NAME = "name";
	public static final String DESCRIPTION = "description";
	public static final String ID = "id";
	public static final String P_DBTEXT = "PDBtext";

}

