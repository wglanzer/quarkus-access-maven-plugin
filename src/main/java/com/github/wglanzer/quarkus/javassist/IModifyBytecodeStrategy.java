package com.github.wglanzer.quarkus.javassist;

import javassist.*;
import lombok.NonNull;

/**
 * Strategy to execute something on a given {@link CtClass}
 *
 * @author w.glanzer, 10.02.2025
 */
interface IModifyBytecodeStrategy
{

  /**
   * Applies all necessary transformations on the given {@link CtClass}
   *
   * @param pCtClass Class that should be transformed
   * @throws NotFoundException            if something failed during transformation
   * @throws ReflectiveOperationException if this strategy failed to transform operations via reflection
   */
  void applyClassTransformation(@NonNull CtClass pCtClass) throws NotFoundException, ReflectiveOperationException;

}
