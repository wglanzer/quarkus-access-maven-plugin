package com.github.wglanzer.quarkus.javassist;

import javassist.*;
import javassist.bytecode.AccessFlag;
import lombok.NonNull;

import java.util.*;

/**
 * Strategy that is able to make fields of a {@link CtClass} public, based on annotations
 *
 * @author w.glanzer, 10.02.2025
 */
class PublicifyStrategy implements IModifyBytecodeStrategy
{

  /**
   * List of packages that contain annotations, which mark methods that should be public
   */
  private static final List<String> PUBLICIFY_ANOOTATIONS_IN_PACKAGES =
      Arrays.asList("jakarta.inject", "jakarta.annotation", "jakarta.decorator", "jakarta.enterprise", "javax.annotation", "io.quarkus.arc");

  @Override
  public void applyClassTransformation(@NonNull CtClass pCtClass) throws NotFoundException, ReflectiveOperationException
  {
    // make fields public
    for (CtField declaredField : pCtClass.getDeclaredFields())
      if (shouldBePublic(declaredField))
        unfrost(pCtClass, () -> declaredField.setModifiers(AccessFlag.setPublic(declaredField.getModifiers())));

    // make methods public
    for (CtMethod declaredMethod : pCtClass.getDeclaredMethods())
      if (shouldBePublic(declaredMethod))
        unfrost(pCtClass, () -> declaredMethod.setModifiers(AccessFlag.setPublic(declaredMethod.getModifiers())));

    // default constructors should be public too
    for (CtConstructor constructors : pCtClass.getDeclaredConstructors())
      if (constructors.getParameterTypes().length == 0)
        unfrost(pCtClass, () -> constructors.setModifiers(AccessFlag.setPublic(constructors.getModifiers())));
  }

  /**
   * Tests, if the given {@link CtMember} should be public
   *
   * @param pMember Member to test
   * @return true, if it should be public
   * @throws ReflectiveOperationException if reflection failed
   */
  private boolean shouldBePublic(@NonNull CtMember pMember) throws ReflectiveOperationException
  {
    for (Object memberAnnotation : pMember.getAnnotations())
      if (PUBLICIFY_ANOOTATIONS_IN_PACKAGES.stream().anyMatch(pPackage -> memberAnnotation.toString().startsWith("@" + pPackage + ".")))
        return true;
    return false;
  }

  /**
   * Unfrosts the given {@link CtClass}, if necessary, and executes the given {@link Runnable}
   *
   * @param pCtClass                  Class that should be defrosted
   * @param pOnDefrostedClassRunnable Runnable that should be executed, if the class has been defrosted successfully
   */
  private void unfrost(@NonNull CtClass pCtClass, @NonNull Runnable pOnDefrostedClassRunnable)
  {
    // unfreeze the class, because we do not care about the class loading anyway
    if (pCtClass.isFrozen())
      pCtClass.defrost();

    // execute the given runnable
    pOnDefrostedClassRunnable.run();
  }

}
