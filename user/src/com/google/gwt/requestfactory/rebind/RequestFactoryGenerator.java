/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.requestfactory.rebind;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.requestfactory.client.impl.AbstractBigDecimalRequest;
import com.google.gwt.requestfactory.client.impl.AbstractBigIntegerRequest;
import com.google.gwt.requestfactory.client.impl.AbstractBooleanRequest;
import com.google.gwt.requestfactory.client.impl.AbstractByteRequest;
import com.google.gwt.requestfactory.client.impl.AbstractCharacterRequest;
import com.google.gwt.requestfactory.client.impl.AbstractDateRequest;
import com.google.gwt.requestfactory.client.impl.AbstractDoubleRequest;
import com.google.gwt.requestfactory.client.impl.AbstractEnumRequest;
import com.google.gwt.requestfactory.client.impl.AbstractFloatRequest;
import com.google.gwt.requestfactory.client.impl.AbstractIntegerRequest;
import com.google.gwt.requestfactory.client.impl.AbstractJsonProxyListRequest;
import com.google.gwt.requestfactory.client.impl.AbstractJsonObjectRequest;
import com.google.gwt.requestfactory.client.impl.AbstractJsonProxySetRequest;
import com.google.gwt.requestfactory.client.impl.AbstractJsonValueListRequest;
import com.google.gwt.requestfactory.client.impl.AbstractLongRequest;
import com.google.gwt.requestfactory.client.impl.AbstractShortRequest;
import com.google.gwt.requestfactory.client.impl.AbstractStringRequest;
import com.google.gwt.requestfactory.client.impl.AbstractVoidRequest;
import com.google.gwt.requestfactory.client.impl.FindRequest;
import com.google.gwt.requestfactory.client.impl.FindRequestObjectImpl;
import com.google.gwt.requestfactory.client.impl.ProxyImpl;
import com.google.gwt.requestfactory.client.impl.ProxyJsoImpl;
import com.google.gwt.requestfactory.client.impl.ProxySchema;
import com.google.gwt.requestfactory.client.impl.ProxyToTypeMap;
import com.google.gwt.requestfactory.client.impl.RequestFactoryJsonImpl;
import com.google.gwt.requestfactory.server.ReflectionBasedOperationRegistry;
import com.google.gwt.requestfactory.shared.ProxySetRequest;
import com.google.gwt.requestfactory.shared.impl.CollectionProperty;
import com.google.gwt.requestfactory.shared.impl.EnumProperty;
import com.google.gwt.requestfactory.shared.impl.Property;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.ProxyListRequest;
import com.google.gwt.requestfactory.shared.ProxyRequest;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.WriteOperation;
import com.google.gwt.requestfactory.shared.impl.RequestData;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.beans.Introspector;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Generates implementations of
 * {@link com.google.gwt.requestfactory.shared.RequestFactory RequestFactory}
 * and its nested interfaces.
 */
public class RequestFactoryGenerator extends Generator {

  private enum CollectionType { 
    SCALAR("ObjectRequestImpl", AbstractJsonObjectRequest.class),
    LIST("ListRequestImpl", AbstractJsonProxyListRequest.class),
    SET("SetRequestImpl", AbstractJsonProxySetRequest.class);

    private String implName;

    private Class<?> requestClass;

    CollectionType(String implName,
        Class<?> requestClass) {
      this.implName = implName;
      this.requestClass = requestClass;
    }

    public String getImplName() {
      return implName;
    }

    public Class<?> getRequestClass() {
      return requestClass;
    }
  }

  private static class EntityProperty {
    private final String name;
    private final JType type;

    public EntityProperty(String name, JType type) {
      this.name = name;
      this.type = type;
    }

    public String getName() {
      return name;
    }

    public JType getType() {
      return type;
    }
  }

  private JClassType listType;
  private JClassType setType;
  private JClassType entityProxyType;

  private final Set<JClassType> generatedProxyTypes = new HashSet<JClassType>();

  @Override
  public String generate(TreeLogger logger, GeneratorContext generatorContext,
      String interfaceName) throws UnableToCompleteException {
    // The TypeOracle knows about all types in the type system
    TypeOracle typeOracle = generatorContext.getTypeOracle();

    listType = typeOracle.findType(List.class.getName());
    setType = typeOracle.findType(Set.class.getName());
    entityProxyType = typeOracle.findType(EntityProxy.class.getName());
    
    // Get a reference to the type that the generator should implement
    JClassType interfaceType = typeOracle.findType(interfaceName);

    // Ensure that the requested type exists
    if (interfaceType == null) {
      logger.log(TreeLogger.ERROR, "Could not find requested typeName: "
          + interfaceName);
      throw new UnableToCompleteException();
    }
    if (interfaceType.isInterface() == null) {
      // The incoming type wasn't a plain interface, we don't support
      // abstract base classes
      logger.log(TreeLogger.ERROR, interfaceType.getQualifiedSourceName()
          + " is not an interface.", null);
      throw new UnableToCompleteException();
    }

    String packageName = interfaceType.getPackage().getName();

    // the replace protects against inner classes
    String implName = interfaceType.getName().replace('.', '_') + "Impl";
    PrintWriter out = generatorContext.tryCreate(logger, packageName, implName);

    // If an implementation already exists, we don't need to do any work
    if (out != null) {
      generateOnce(
          typeOracle.findType(RequestFactory.class.getCanonicalName()), logger,
          generatorContext, out, interfaceType, packageName, implName);
    }

    return packageName + "." + implName;
  }

  private String asInnerImplClass(String className, JClassType outerClassName) {
    className = outerClassName.getQualifiedSourceName() + "Impl." + className;
    return className;
  }

  private String capitalize(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  /**
   * Compute the list of EntityProperties from the given proxy type. Properties
   * contributed from EntityProxy are excluded.
   */
  private List<EntityProperty> computeEntityPropertiesFromProxyType(
      JClassType publicProxyType, TreeLogger logger)
      throws UnableToCompleteException {
    List<EntityProperty> entityProperties = new ArrayList<EntityProperty>();
    Set<String> propertyNames = new HashSet<String>();

    for (JMethod method : publicProxyType.getOverridableMethods()) {
      if (method.getEnclosingType() == entityProxyType) {
        // Properties on EntityProxy are handled by ProxyJsoImpl
        continue;
      }

      EntityProperty entityProperty = maybeComputePropertyFromMethod(method, logger);
      if (entityProperty != null) {
        String propertyName = entityProperty.getName();
        if (!propertyNames.contains(propertyName)) {
          propertyNames.add(propertyName);
          entityProperties.add(entityProperty);
        }
      }
    }

    return entityProperties;
  };
  
  private void ensureProxyType(TreeLogger logger,
      GeneratorContext generatorContext, JClassType publicProxyType)
      throws UnableToCompleteException {
    TypeOracle typeOracle = generatorContext.getTypeOracle();

    if (!publicProxyType.isAssignableTo(entityProxyType)) {
      return;
    }

    if (publicProxyType.equals(entityProxyType)) {
      return;
    }
    if (generatedProxyTypes.contains(publicProxyType)) {
      return;
    }
    String packageName = publicProxyType.getPackage().getName();

    String proxyImplTypeName = publicProxyType.getName() + "Impl";
    PrintWriter pw = generatorContext.tryCreate(logger, packageName,
        proxyImplTypeName);

    Set<JClassType> transitiveDeps = new LinkedHashSet<JClassType>();

    if (pw != null) {
      logger = logger.branch(TreeLogger.DEBUG,
          "Generating " + publicProxyType.getName());

      ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
          packageName, proxyImplTypeName);

      f.addImport(AbstractJsonProxySetRequest.class.getName());  
      f.addImport(AbstractJsonProxyListRequest.class.getName());
      f.addImport(AbstractJsonValueListRequest.class.getName());      
      f.addImport(AbstractJsonObjectRequest.class.getName());
      f.addImport(RequestFactoryJsonImpl.class.getName());
      f.addImport(Property.class.getName());
      f.addImport(EnumProperty.class.getName());
      f.addImport(CollectionProperty.class.getName());
      
      f.addImport(EntityProxy.class.getName());
      f.addImport(ProxyImpl.class.getName());
      f.addImport(ProxyJsoImpl.class.getName());
      f.addImport(ProxySchema.class.getName());
      f.addImport(WriteOperation.class.getName().replace("$", "."));

      f.addImport(Collections.class.getName());
      f.addImport(HashSet.class.getName());
      f.addImport(Set.class.getName());

      f.setSuperclass(ProxyImpl.class.getSimpleName());
      f.addImplementedInterface(publicProxyType.getName());

      List<EntityProperty> entityProperties = computeEntityPropertiesFromProxyType(publicProxyType, logger);
      for (EntityProperty entityProperty : entityProperties) {
        JType type = entityProperty.getType();
        if (type.isPrimitive() == null) {
          f.addImport(type.getErasedType().getQualifiedSourceName());
        }
      }

      SourceWriter sw = f.createSourceWriter(generatorContext, pw);
      sw.println();

      // write the Property fields
      for (EntityProperty entityProperty : entityProperties) {
        sw.println();
        String name = entityProperty.getName();
        if (entityProperty.getType().isEnum() != null) {
          sw.println(String.format(
              "private static final Property<%1$s> %2$s = new EnumProperty<%1$s>(\"%2$s\", %1$s.class, %1$s.values());",
              entityProperty.getType().getSimpleSourceName(),
              name));
        } else if (isCollection(typeOracle, entityProperty.getType())) {
          sw.println(String.format(
              "private static final Property<%1$s> %2$s = new CollectionProperty<%1$s, %3$s>(\"%2$s\", %1$s.class, %3$s.class);",
              entityProperty.getType().getSimpleSourceName(),
              name,
              entityProperty.getType().isParameterized().getTypeArgs()[0].getQualifiedSourceName()
              ));
        } else {
          sw.println(String.format(
              "private static final Property<%1$s> %2$s = new Property<%1$s>(\"%2$s\", \"%3$s\", %1$s.class);",
              entityProperty.getType().getSimpleSourceName(), name,
              capitalize(name)));
        }
      }

      printSchema(typeOracle, publicProxyType, proxyImplTypeName, sw, logger);

      sw.println();
      String simpleImplName = publicProxyType.getSimpleSourceName() + "Impl";
      printRequestImplClass(sw, publicProxyType, simpleImplName, CollectionType.LIST);
      printRequestImplClass(sw, publicProxyType, simpleImplName, CollectionType.SET);
      printRequestImplClass(sw, publicProxyType, simpleImplName, CollectionType.SCALAR);

      sw.println();
      sw.println(String.format(
          "public static final ProxySchema<%s> SCHEMA = new MySchema();",
          proxyImplTypeName));

      sw.println();
      sw.println(String.format(
          "private %s(ProxyJsoImpl jso, boolean isFuture) {", proxyImplTypeName));
      sw.indent();
      sw.println("super(jso, isFuture);");
      sw.outdent();
      sw.println("}");

      // getter and setter methods
      for (EntityProperty entityProperty : entityProperties) {
        JClassType returnType = entityProperty.getType().isClassOrInterface();
        String returnTypeString = returnType.getQualifiedSourceName();
        JClassType collectionType = returnType.isClassOrInterface();

        if (collectionType != null && collectionType.isParameterized() != null) {
           returnType = collectionType.isParameterized().getTypeArgs()[0];
           returnTypeString = collectionType.isParameterized().getParameterizedQualifiedSourceName();
        }

        sw.println();
        sw.println(String.format("public %s get%s() {",
            returnTypeString,
            capitalize(entityProperty.getName())));
        sw.indent();
        sw.println(String.format("return get(%s);", entityProperty.getName()));
        sw.outdent();
        sw.println("}");

        sw.println();
        String varName = entityProperty.getName();
        sw.println(String.format("public void set%s(%s %s) {",
            capitalize(varName), returnTypeString, varName));
        sw.indent();
        sw.println(String.format("set(this.%s, this, %s);", varName, varName));
        sw.outdent();
        sw.println("}");
        /*
         * Because a Proxy A may relate to B which relates to C, we need to
         * ensure transitively.
         */
        if (isProxyType(typeOracle, returnType)) {
          transitiveDeps.add(returnType);
        }
      }

      sw.outdent();
      sw.println("}");
      generatorContext.commit(logger, pw);
    }

    generatedProxyTypes.add(publicProxyType);
    // ensure generatation of transitive dependencies
    for (JClassType type : transitiveDeps) {
      ensureProxyType(logger, generatorContext, type);
    }
  }

  private void generateOnce(JClassType requestFactoryType, TreeLogger logger,
      GeneratorContext generatorContext, PrintWriter out,
      JClassType interfaceType, String packageName, String implName)
      throws UnableToCompleteException {

    logger = logger.branch(
        TreeLogger.DEBUG,
        String.format("Generating implementation of %s",
            interfaceType.getName()));

    ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
        packageName, implName);
    f.addImport(HandlerManager.class.getName());
    f.addImport(RequestFactoryJsonImpl.class.getName());
    f.addImport(interfaceType.getQualifiedSourceName());
    f.addImport(ProxyToTypeMap.class.getName());
    f.addImport(EntityProxy.class.getName());
    f.addImport(EntityProxyId.class.getName());
    f.addImport(ProxySchema.class.getName());
    f.addImplementedInterface(interfaceType.getName());
    f.setSuperclass(RequestFactoryJsonImpl.class.getSimpleName());

    SourceWriter sw = f.createSourceWriter(generatorContext, out);
    sw.println();

    // Find the requestSelector methods
    // TODO allow getRequest type methods to live directly on the factory, w/o
    // requiring a selector to get to them
    // TODO rename variable this requestBuilders, holding off to avoid merge
    // hell
    Set<JMethod> requestSelectors = new LinkedHashSet<JMethod>();
    for (JMethod method : interfaceType.getOverridableMethods()) {
      if (method.getEnclosingType().equals(requestFactoryType)) {
        continue;
      }
      JType returnType = method.getReturnType();
      if (null == returnType) {
        logger.log(TreeLogger.ERROR, String.format(
            "Illegal return type for %s. Methods of %s must return interfaces, found void",
                method.getName(), interfaceType.getName()));
        throw new UnableToCompleteException();
      }
      JClassType asInterface = returnType.isInterface();
      if (null == asInterface) {
        logger.log(TreeLogger.ERROR, String.format(
            "Illegal return type for %s. Methods of %s must return interfaces",
            method.getName(), interfaceType.getName()));
        throw new UnableToCompleteException();
      }
      requestSelectors.add(method);
    }
    /*
     * Hard-code the requestSelectors specified in RequestFactory.
     */
    JClassType t = generatorContext.getTypeOracle().findType(
        RequestFactoryJsonImpl.class.getName());
    try {
      requestSelectors.add(t.getMethod("findRequest", new JType[0]));
    } catch (NotFoundException e) {
      e.printStackTrace();
    }

    JClassType proxyToTypeInterface = generatorContext.getTypeOracle().findType(
        ProxyToTypeMap.class.getName());
    // TODO: note, this seems like a bug. What if you have 2 RequestFactories?
    String proxyToTypeMapName = proxyToTypeInterface.getName() + "Impl";

    // write create(Class)
    sw.println("public " + EntityProxy.class.getName()
        + " create(Class token) {");
    sw.indent();
    sw.println("return create(token, new " + proxyToTypeMapName + "());");
    sw.outdent();
    sw.println("}");
    sw.println();

    // write getHistoryToken(proxyId)
    sw.println("public String getHistoryToken(EntityProxyId proxyId) {");
    sw.indent();
    sw.println("return getHistoryToken(proxyId, new " + proxyToTypeMapName + "());");
    sw.outdent();
    sw.println("}");
    sw.println();

    // write getToken(Class)
    sw.println("public String getToken(Class clazz) {");
    sw.indent();
    sw.println("return new " + proxyToTypeMapName + "().getClassToken(clazz);");
    sw.outdent();
    sw.println("}");
    sw.println();

    // write getClass(String)
    sw.println("public Class<? extends " + EntityProxy.class.getName()
        + "> getClass(String token) {");
    sw.indent();
    sw.println("return getClass(token, new " + proxyToTypeMapName + "());");
    sw.outdent();
    sw.println("}");
    sw.println();

    // write getProxyId(String)
    sw.println("public " + EntityProxyId.class.getName()
        + " getProxyId(String token) {");
    sw.indent();
    sw.println("return getProxyId(token, new " + proxyToTypeMapName + "());");
    sw.outdent();
    sw.println("}");
    sw.println();

    sw.println("public ProxySchema<? extends EntityProxy> getSchema(String schemaToken) {");
    sw.indent();
    sw.println("return new " + proxyToTypeMapName + "().getType(schemaToken);");
    sw.outdent();
    sw.println("}");

    // write a method for each request builder and generate it
    for (JMethod requestSelector : requestSelectors) {
      String returnTypeName = requestSelector.getReturnType().getQualifiedSourceName();
      String nestedImplName = capitalize(requestSelector.getName().replace('.',
          '_'))
          + "Impl";
      String nestedImplPackage = generatorContext.getTypeOracle().findType(
          returnTypeName).getPackage().getName();

      sw.println("public " + returnTypeName + " " + requestSelector.getName()
          + "() {");
      sw.indent();
      sw.println("return new " + nestedImplPackage + "." + nestedImplName
          + "(this);");
      sw.outdent();
      sw.println("}");
      sw.println();

      PrintWriter pw = generatorContext.tryCreate(logger, nestedImplPackage,
          nestedImplName);
      if (pw != null) {
        generateRequestSelectorImplementation(logger, generatorContext, pw,
            requestSelector, interfaceType, nestedImplPackage, nestedImplName);
      }
    }

    // close the class
    sw.outdent();
    sw.println("}");

    // generate the mapping type implementation
    PrintWriter pw = generatorContext.tryCreate(logger, packageName,
        proxyToTypeMapName);
    if (pw != null) {
      generateProxyToTypeMap(logger, generatorContext, pw,
          proxyToTypeInterface, packageName, proxyToTypeMapName);
    }
    generatorContext.commit(logger, out);
  }

  private void generateProxyToTypeMap(TreeLogger logger,
      GeneratorContext generatorContext, PrintWriter out,
      JClassType interfaceType, String packageName, String implName) {
    logger = logger.branch(
        TreeLogger.DEBUG,
        String.format("Generating implementation of %s",
            interfaceType.getName()));

    ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
        packageName, implName);
    f.addImport(interfaceType.getQualifiedSourceName());
    f.addImport(ProxyImpl.class.getName());
    f.addImport(ProxySchema.class.getName());
    f.addImport(interfaceType.getQualifiedSourceName());
    f.addImplementedInterface(interfaceType.getName());

    f.addImplementedInterface(interfaceType.getName());

    SourceWriter sw = f.createSourceWriter(generatorContext, out);
    sw.println();

    sw.println("@SuppressWarnings(\"unchecked\")");
    sw.println("public <R extends ProxyImpl> ProxySchema<R> getType(Class<R> proxyClass) {");
    sw.indent();
    for (JClassType publicProxyType : generatedProxyTypes) {
      String qualifiedSourceName = publicProxyType.getQualifiedSourceName();
      sw.println("if (proxyClass == " + qualifiedSourceName + ".class) {");
      sw.indent();
      sw.println("return (ProxySchema<R>) " + qualifiedSourceName
          + "Impl.SCHEMA;");
      sw.outdent();
      sw.println("}");
    }
    sw.println("throw new IllegalArgumentException(\"Unknown proxyClass \" + proxyClass);");
    sw.indent();
    sw.outdent();
    sw.outdent();
    sw.println("}");

    sw.println("public ProxySchema<? extends ProxyImpl> getType(String token) {");
    sw.indent();
    sw.println("String[] bits = token.split(\"@\");");
    for (JClassType publicProxyType : generatedProxyTypes) {
      String qualifiedSourceName = publicProxyType.getQualifiedSourceName();
      sw.println("if (bits[0].equals(\"" + qualifiedSourceName + "\")) {");
      sw.indent();
      sw.println("return " + qualifiedSourceName + "Impl.SCHEMA;");
      sw.outdent();
      sw.println("}");
    }
    sw.println("throw new IllegalArgumentException(\"Unknown string token: \" + token);");
    sw.outdent();
    sw.println("}");

    sw.println("public String getClassToken(Class<?> proxyClass) {");
    sw.indent();
    for (JClassType publicProxyType : generatedProxyTypes) {
      String qualifiedSourceName = publicProxyType.getQualifiedSourceName();
      sw.println("if (proxyClass == " + qualifiedSourceName + ".class) {");
      sw.indent();
      sw.println("return \"" + qualifiedSourceName + "\";");
      sw.outdent();
      sw.println("}");
    }
    sw.println("throw new IllegalArgumentException(\"Unknown proxyClass \" + proxyClass);");
    sw.indent();
    sw.outdent();
    sw.outdent();
    sw.println("}");

    sw.outdent();
    sw.println("}");
    sw.println();

    generatorContext.commit(logger, out);
  }

  private void generateRequestSelectorImplementation(TreeLogger logger,
      GeneratorContext generatorContext, PrintWriter out,
      JMethod selectorMethod, JClassType mainType, String packageName,
      String implName) throws UnableToCompleteException {
    JClassType selectorInterface = selectorMethod.getReturnType().isInterface();
    logger = logger.branch(
        TreeLogger.DEBUG,
        String.format("Generating implementation of %s",
            selectorInterface.getName()));

    ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
        packageName, implName);
    f.addImport(RequestData.class.getName());
    f.addImport(mainType.getQualifiedSourceName() + "Impl");
    f.addImplementedInterface(selectorInterface.getQualifiedSourceName());

    SourceWriter sw = f.createSourceWriter(generatorContext, out);
    sw.println();

    sw.println("private final " + mainType.getName() + "Impl factory;");
    sw.println();
    // constructor for the class.
    sw.println("public " + implName + "(" + mainType.getName()
        + "Impl factory) {");
    sw.indent();
    sw.println("this.factory = factory;");
    sw.outdent();
    sw.println("}");
    sw.println();

    // write each method.
    for (JMethod method : selectorInterface.getOverridableMethods()) {
      JParameterizedType parameterizedType = method.getReturnType()
          .isParameterized();
      if (parameterizedType == null) {
        logger.log(TreeLogger.ERROR, String.format(
            "Illegal return type for %s. Methods of %s must return Request<T>.",
            method.getName(), selectorInterface.getName()));
        throw new UnableToCompleteException();
      }
      JClassType returnType = parameterizedType.getTypeArgs()[0];

      ensureProxyType(logger, generatorContext, returnType);

      String operationName = selectorInterface.getQualifiedBinaryName()
          + ReflectionBasedOperationRegistry.SCOPE_SEPARATOR + method.getName();

      JClassType requestType = method.getReturnType().isClassOrInterface();
      String requestClassName = null;

      TypeOracle typeOracle = generatorContext.getTypeOracle();
      String extraArgs = "";
      // TODO: refactor this into some kind of extensible map lookup
      // check for ProxyListRequest<T> or ProxySetRequest<T>
      if (method.getReturnType().isArray() != null) {
        logger.log(TreeLogger.ERROR, String.format(
            "Illegal return type for %s. Methods of %s cannot return array.",
            method.getName(), selectorInterface.getName()));
        throw new UnableToCompleteException();
      }
      for (JParameter param : method.getParameters()) {
        if (param.getType().isArray() != null) {
          logger.log(TreeLogger.ERROR, String.format(
            "Illegal param type %s for %s. Methods of %s cannot take array parameters.",
            param.getName(), method.getName(), selectorInterface.getName()));
          throw new UnableToCompleteException();
        }
      }
      if (isProxyCollectionRequest(typeOracle, requestType)) {
        Class<?> colType = getCollectionType(typeOracle, requestType);
        assert colType != null;
        requestClassName = asInnerImplClass(colType == List.class ?
            "ListRequestImpl" : "SetRequestImpl", returnType);
      } else if (isProxyRequest(typeOracle, requestType)) {
        if (selectorInterface.isAssignableTo(typeOracle.findType(FindRequest.class.getName()))) {
          extraArgs = ", proxyId";
          requestClassName = FindRequestObjectImpl.class.getName();
        } else {
          requestClassName = asInnerImplClass("ObjectRequestImpl", returnType);
        }
      } else if (isValueListRequest(typeOracle, requestType)) {
         requestClassName = AbstractJsonValueListRequest.class.getName();
         // generate argument list for AbstractJsonValueListRequest constructor
         JClassType colType = requestType.isParameterized().getTypeArgs()[0];
         extraArgs = ", " + colType.isAssignableTo(setType);
         // extraArgs = ", isSet"  true if elementType of collection is Set
         JClassType leafType = colType.isParameterized().getTypeArgs()[0];
         extraArgs += ", " + leafType.getQualifiedSourceName() + ".class";
         // extraArgs = ", isSet, collectionElementType.class"
        if (leafType.isAssignableTo(typeOracle.findType(Enum.class.getName()))) {
          // if contained type is Enum, pass Enum.values()
          extraArgs += ", " + leafType.getQualifiedSourceName() + ".values()"; 
        } else {
          // else for all other types, pass null
          extraArgs += ", null";
        }
      } else if (isStringRequest(typeOracle, requestType)) {
        requestClassName = AbstractStringRequest.class.getName();
      } else if (isLongRequest(typeOracle, requestType)) {
        requestClassName = AbstractLongRequest.class.getName();
      } else if (isIntegerRequest(typeOracle, requestType)) {
        requestClassName = AbstractIntegerRequest.class.getName();
      } else if (isDoubleRequest(typeOracle, requestType)) {
        requestClassName = AbstractDoubleRequest.class.getName();
      } else if (isByteRequest(typeOracle, requestType)) {
        requestClassName = AbstractByteRequest.class.getName();
      } else if (isBooleanRequest(typeOracle, requestType)) {
        requestClassName = AbstractBooleanRequest.class.getName();
      } else if (isShortRequest(typeOracle, requestType)) {
        requestClassName = AbstractShortRequest.class.getName();
      } else if (isFloatRequest(typeOracle, requestType)) {
        requestClassName = AbstractFloatRequest.class.getName();
      } else if (isCharacterRequest(typeOracle, requestType)) {
        requestClassName = AbstractCharacterRequest.class.getName();
      } else if (isDateRequest(typeOracle, requestType)) {
        requestClassName = AbstractDateRequest.class.getName();
      } else if (isBigDecimalRequest(typeOracle, requestType)) {
        requestClassName = AbstractBigDecimalRequest.class.getName();
      } else if (isBigIntegerRequest(typeOracle, requestType)) {
        requestClassName = AbstractBigIntegerRequest.class.getName();
      } else if (isEnumRequest(typeOracle, requestType)) {
        requestClassName = AbstractEnumRequest.class.getName();
        JClassType enumType = requestType.isParameterized().getTypeArgs()[0];
        extraArgs = ", " + enumType.getQualifiedSourceName()
            + ".values()";
      } else if (isVoidRequest(typeOracle, requestType)) {
        requestClassName = AbstractVoidRequest.class.getName();
      } else {
        logger.log(TreeLogger.ERROR, "Return type " + requestType
            + " is not yet supported");
        throw new UnableToCompleteException();
      }

      sw.println(getMethodDeclaration(method) + " {");
      sw.indent();
      sw.println(
          "return new " + requestClassName + "(factory" + extraArgs + ") {");
      sw.indent();
      String requestDataName = RequestData.class.getSimpleName();
      sw.println("public " + requestDataName + " getRequestData() {");
      sw.indent();
      sw.println("return new " + requestDataName + "(\"" + operationName
          + "\", " + getParametersAsString(method, typeOracle) + ", "
          + "getPropertyRefs());");
      sw.outdent();
      sw.println("}");
      sw.outdent();
      sw.println("};");
      sw.outdent();
      sw.println("}");
    }

    sw.outdent();
    sw.println("}");
    generatorContext.commit(logger, out);
  }

  /**
   * If requestType is ProxyListRequest or RequestObject<List<T>> return List,
   * otherwise if requestType is ProxySetRequest or RequestObject<Set<T>> return
   * Set, otherwise return null.
   */
  private Class<?> getCollectionType(TypeOracle typeOracle,
      JClassType requestType) {
    if (requestType.isAssignableTo(
        typeOracle.findType(ProxyListRequest.class.getName()))) {
      return List.class;
    }
    if (requestType.isAssignableTo(typeOracle.findType(
        ProxySetRequest.class.getName()))) {
      return Set.class;
    }
    JClassType retType = requestType.isParameterized().getTypeArgs()[0];
    if (retType.isParameterized() != null) {
      JClassType leafType = retType.isParameterized().getTypeArgs()[0];
      if (isProxyType(typeOracle, leafType)) {
        if (retType.isAssignableTo(listType)) {
          return List.class;
        } else if (retType.isAssignableTo(setType)) {
          return Set.class;
        }
      }
    }
    return null;
  }

  /**
   * This method is very similar to {@link
   * com.google.gwt.core.ext.typeinfo.JMethod.getReadableDeclaration()}. The
   * only change is that each parameter is final.
   */
  private String getMethodDeclaration(JMethod method) {
    StringBuilder sb = new StringBuilder("public ");
    sb.append(method.getReturnType().getParameterizedQualifiedSourceName());
    sb.append(" ");
    sb.append(method.getName());
    sb.append("(");

    boolean needComma = false;
    for (JParameter param : method.getParameters()) {
      if (needComma) {
        sb.append(", ");
      } else {
        needComma = true;
      }
      sb.append("final "); // so that an anonymous class can refer it
      sb.append(param.getType().getParameterizedQualifiedSourceName());
      sb.append(" ");
      sb.append(param.getName());
    }
    sb.append(")");
    return sb.toString();
  }

  /**
   * Returns the string representation of the parameters to be passed to the
   * server side method.
   */
  private String getParametersAsString(JMethod method, TypeOracle typeOracle) {
    StringBuilder sb = new StringBuilder();
    for (JParameter parameter : method.getParameters()) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      JClassType classType = parameter.getType().isClassOrInterface();

      JType paramType = parameter.getType();
      if (paramType.getQualifiedSourceName().equals(EntityProxyId.class.getName())) {
        sb.append("factory.getWireFormat(" + parameter.getName() + ")");
        continue;
      }
      
      if (classType != null && classType.isAssignableTo(entityProxyType)) {
        sb.append("((" + classType.getQualifiedBinaryName() + "Impl" + ")");
      }
      sb.append(parameter.getName());
      if (classType != null && classType.isAssignableTo(entityProxyType)) {
        sb.append(").wireFormatId()");
      }
    }
    return "new Object[] {" + sb.toString() + "}";
  }

  private boolean isBigDecimalRequest(TypeOracle typeOracle,
      JClassType requestType) {
    return requestType.isParameterized().getTypeArgs()[0].isAssignableTo(typeOracle.findType(BigDecimal.class.getName()));
  }

  private boolean isBigIntegerRequest(TypeOracle typeOracle,
      JClassType requestType) {
    return requestType.isParameterized().getTypeArgs()[0].isAssignableTo(typeOracle.findType(BigInteger.class.getName()));
  }

  private boolean isBooleanRequest(TypeOracle typeOracle, JClassType requestType) {
    return requestType.isParameterized().getTypeArgs()[0].isAssignableTo(typeOracle.findType(Boolean.class.getName()));
  }

  private boolean isByteRequest(TypeOracle typeOracle, JClassType requestType) {
    return requestType.isParameterized().getTypeArgs()[0].isAssignableTo(typeOracle.findType(Byte.class.getName()));
  }

  private boolean isCharacterRequest(TypeOracle typeOracle,
      JClassType requestType) {
    return requestType.isParameterized().getTypeArgs()[0].isAssignableTo(typeOracle.findType(Character.class.getName()));
  }

  private boolean isCollection(TypeOracle typeOracle,
      JType requestType) {
    return requestType.isParameterized() != null
            && requestType.isParameterized().isAssignableTo(
        typeOracle.findType(Collection.class.getName()));
  }

  private boolean isDateRequest(TypeOracle typeOracle, JClassType requestType) {
    return requestType.isParameterized().getTypeArgs()[0].isAssignableTo(typeOracle.findType(
        Date.class.getName()));
  }

  private boolean isDoubleRequest(TypeOracle typeOracle, JClassType requestType) {
    return requestType.isParameterized().getTypeArgs()[0].isAssignableTo(typeOracle.findType(Double.class.getName()));
  }

  private boolean isEnumRequest(TypeOracle typeOracle, JClassType requestType) {
    return requestType.isParameterized().getTypeArgs()[0].isAssignableTo(typeOracle.findType(Enum.class.getName()));
  }

  private boolean isFloatRequest(TypeOracle typeOracle, JClassType requestType) {
    return requestType.isParameterized().getTypeArgs()[0].isAssignableTo(typeOracle.findType(Float.class.getName()));
  }

  private boolean isIntegerRequest(TypeOracle typeOracle, JClassType requestType) {
    return requestType.isParameterized().getTypeArgs()[0].isAssignableTo(typeOracle.findType(Integer.class.getName()));
  }

  private boolean isLongRequest(TypeOracle typeOracle, JClassType requestType) {
    return requestType.isParameterized().getTypeArgs()[0].isAssignableTo(typeOracle.findType(Long.class.getName()));
  }

  private boolean isProxyCollectionRequest(TypeOracle typeOracle,
      JClassType requestType) {
    return requestType.isAssignableTo(typeOracle.findType(
        ProxyListRequest.class.getName()))
        || requestType.isAssignableTo(typeOracle.findType(
        ProxySetRequest.class.getName()));
  }
  
  private boolean isProxyRequest(TypeOracle typeOracle, JClassType requestType) {
    return requestType.isAssignableTo(typeOracle.findType(ProxyRequest.class.getName()));
  }

  private boolean isProxyType(TypeOracle typeOracle, JClassType requestType) {
    return requestType.isAssignableTo(entityProxyType);
  }

  private boolean isRequestObjectCollectionRequest(TypeOracle typeOracle,
      JClassType requestType) {
    JClassType retType = requestType.isParameterized().getTypeArgs()[0];
    if (retType.isAssignableTo(listType) || retType.isAssignableTo(setType)) {
      if (retType.isParameterized() != null) {
        JClassType leafType = retType.isParameterized().getTypeArgs()[0];
        return isProxyType(typeOracle, leafType);
      }
    }
    return false;
  }

  private boolean isShortRequest(TypeOracle typeOracle, JClassType requestType) {
    return requestType.isParameterized().getTypeArgs()[0].isAssignableTo(typeOracle.findType(Short.class.getName()));
  }

  private boolean isStringRequest(TypeOracle typeOracle, JClassType requestType) {
    return requestType.isParameterized().getTypeArgs()[0].isAssignableTo(typeOracle.findType(String.class.getName()));
  }

  private boolean isValueListRequest(TypeOracle typeOracle,
      JClassType requestType) {
    JClassType retType = requestType.isParameterized().getTypeArgs()[0];
    if (retType.isAssignableTo(listType) || retType.isAssignableTo(setType)) {
      if (retType.isParameterized() != null) {
        JClassType leafType = retType.isParameterized().getTypeArgs()[0];
        return !isProxyType(typeOracle, leafType);
      }
    }
    return false;
  }

  private boolean isVoidRequest(TypeOracle typeOracle, JClassType requestType) {
    return requestType.isParameterized().getTypeArgs()[0].isAssignableTo(typeOracle.findType(Void.class.getName()));
  }

  /**
   * Returns an {@link EntityProperty} if the method looks like a bean property
   * accessor or <code>null</code>.
   */
  private EntityProperty maybeComputePropertyFromMethod(JMethod method,
      TreeLogger logger) throws UnableToCompleteException {
    String propertyName = null;
    JType propertyType = null;
    String methodName = method.getName();
    if (methodName.startsWith("get")) {
      propertyName = Introspector.decapitalize(methodName.substring(3));
      propertyType = method.getReturnType();
      if (propertyType.isArray() != null) {
        logger.log(TreeLogger.ERROR, "Method " + methodName
        + " on " + method.getEnclosingType().getQualifiedSourceName()
        + " may not return a Java array.");
        throw new UnableToCompleteException();
      }
    } else if (methodName.startsWith("set")) {
      propertyName = Introspector.decapitalize(methodName.substring(3));
      JParameter[] parameters = method.getParameters();
      if (parameters.length > 0) {
        propertyType = parameters[parameters.length - 1].getType();
        if (propertyType.isArray() != null) {
          logger.log(TreeLogger.ERROR, "Method " + methodName
          + " on " + method.getEnclosingType().getQualifiedSourceName()
          + " may accept a Java array as a parameter.");
          throw new UnableToCompleteException();
        }
      }
    }

    // TODO: handle boolean "is" getters, indexed properties?

    if (propertyType != null && propertyType != JPrimitiveType.VOID) {
      return new EntityProperty(propertyName, propertyType);
    }

    return null;
  }

  /**
   * Prints a ListRequestImpl or ObjectRequestImpl class.
   */
  private void printRequestImplClass(SourceWriter sw, JClassType returnType,
      String returnImplTypeName, CollectionType collection) {

    String name = collection.getImplName();
    Class<?> superClass = collection.getRequestClass();

    sw.println("public static abstract class " + name + " extends "
        + superClass.getSimpleName() + "<" + returnType.getName() + ", " + name
        + "> {");
    sw.println();
    sw.indent();

    sw.println(String.format("%s(%s factory) {", name,
        RequestFactoryJsonImpl.class.getSimpleName()));
    sw.indent();
    sw.println("super(" + returnImplTypeName + ".SCHEMA, factory);");
    sw.outdent();
    sw.println("}");
    sw.println();
    sw.println("@Override");
    sw.println("protected " + name + " getThis() {");
    sw.indent();
    sw.println("return this;");
    sw.outdent();
    sw.println("}");

    sw.outdent();
    sw.println("}");
    sw.println();
  }

  /**
   * @param typeOracle
   * @param publicProxyType
   * @param proxyImplTypeName
   * @param sw
   * @param logger
   * @return
   * @throws UnableToCompleteException
   */

  private JClassType printSchema(TypeOracle typeOracle,
      JClassType publicProxyType, String proxyImplTypeName, SourceWriter sw,
      TreeLogger logger) throws UnableToCompleteException {
    sw.println(String.format(
        "public static class MySchema extends ProxySchema<%s> {",
        proxyImplTypeName));

    sw.indent();
    sw.println("private final Set<Property<?>> allProperties;");
    sw.println("{");

    sw.indent();
    sw.println("Set<Property<?>> set = new HashSet<Property<?>>();");
    sw.println("set.addAll(super.allProperties());");

    JClassType propertyType;
    try {
      propertyType = typeOracle.getType(Property.class.getName());
    } catch (NotFoundException e) {
      throw new RuntimeException(e);
    }

    List<EntityProperty> entityProperties = computeEntityPropertiesFromProxyType(publicProxyType,
        logger);
    for (EntityProperty entityProperty : entityProperties) {
      sw.println(String.format("set.add(%s);", entityProperty.getName()));
    }

    sw.println("allProperties = Collections.unmodifiableSet(set);");
    sw.outdent();
    sw.println("}");

    sw.println();
    sw.println("public Set<Property<?>> allProperties() {");
    sw.indent();
    sw.println("return allProperties;");
    sw.outdent();
    sw.println("}");

    sw.println();
    sw.println("public MySchema() {");
    sw.indent();
    sw.println("super(\"" + publicProxyType.getQualifiedSourceName() + "\");");
    sw.outdent();
    sw.println("}");

    sw.println();
    sw.println("@Override");
    sw.println(String.format(
        "public %s create(ProxyJsoImpl jso, boolean isFuture) {",
        proxyImplTypeName));
    sw.indent();
    sw.println(String.format("return new %s(jso, isFuture);", proxyImplTypeName));
    sw.outdent();
    sw.println("}");

    sw.println();
    sw.println("public Class getProxyClass() {");
    sw.indent();
    sw.println("return " + publicProxyType.getQualifiedSourceName() + ".class;"
        + " // special field");
    sw.outdent();
    sw.println("}");

    sw.outdent();
    sw.println("}");
    return propertyType;
  }
}
