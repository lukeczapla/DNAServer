package edu.dnatools.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(BoundaryCondition.class)
public abstract class BoundaryCondition_ {

	public static volatile SingularAttribute<BoundaryCondition, Double> slide;
	public static volatile SingularAttribute<BoundaryCondition, Double> shift;
	public static volatile SingularAttribute<BoundaryCondition, String> name;
	public static volatile SingularAttribute<BoundaryCondition, Double> roll;
	public static volatile SingularAttribute<BoundaryCondition, Long> id;
	public static volatile SingularAttribute<BoundaryCondition, Double> tilt;
	public static volatile SingularAttribute<BoundaryCondition, Double> rise;
	public static volatile SingularAttribute<BoundaryCondition, User> user;
	public static volatile SingularAttribute<BoundaryCondition, Double> twist;

	public static final String SLIDE = "slide";
	public static final String SHIFT = "shift";
	public static final String NAME = "name";
	public static final String ROLL = "roll";
	public static final String ID = "id";
	public static final String TILT = "tilt";
	public static final String RISE = "rise";
	public static final String USER = "user";
	public static final String TWIST = "twist";

}

