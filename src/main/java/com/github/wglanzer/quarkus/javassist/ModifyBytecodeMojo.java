package com.github.wglanzer.quarkus.javassist;

import javassist.*;
import lombok.NonNull;
import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * @author w.glanzer, 10.02.2025
 */
@Mojo(name = "modify", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ModifyBytecodeMojo extends AbstractMojo
{

  private static final List<IModifyBytecodeStrategy> STRATEGIES = Collections.singletonList(new PublicifyStrategy());

  /**
   * the {@link MavenProject} that is currectly in build
   */
  @Parameter(defaultValue = "${project}", property = "modify.project", required = true, readonly = true)
  private MavenProject project;

  @Override
  public void execute() throws MojoExecutionException
  {
    ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();

    try
    {
      // create our own class loader, that loads classes from the current build too
      List<URL> classPath = new ArrayList<>();
      for (String runtimeResource : project.getRuntimeClasspathElements())
        classPath.add(new File(runtimeResource).toURI().toURL());
      classPath.add(new File(project.getBuild().getOutputDirectory()).toURI().toURL());
      URLClassLoader classLoader = URLClassLoader.newInstance(classPath.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
      Thread.currentThread().setContextClassLoader(classLoader);

      // create a new class pool to resolve our classes currently in build
      ClassPool classPool = new ClassPool(ClassPool.getDefault());
      classPool.childFirstLookup = true;
      classPool.appendClassPath(project.getBuild().getOutputDirectory());
      classPool.appendClassPath(new LoaderClassPath(classLoader));
      classPool.appendSystemPath();

      // transform all classes
      List<String> classNames = getClassNames(project.getBuild().getOutputDirectory());
      for (String className : classNames)
        transformClass(classPool, project.getBuild().getOutputDirectory(), className);

      // log, what we did here
      getLog().info("Transformed " + classNames.size() + " classes.");
    }
    catch (Exception e)
    {
      throw new MojoExecutionException(e.getMessage(), e);
    }
    finally
    {
      // reset the class loader
      Thread.currentThread().setContextClassLoader(originalContextClassLoader);
    }
  }

  /**
   * Search for class files (file extension: {@code .class}) on the passed {@code directory}.
   *
   * @param pDirectory path to a directory to search all class names for
   * @return List of full qualified class names
   */
  @NonNull
  private List<String> getClassNames(@NonNull String pDirectory) throws IOException
  {
    File dir = new File(pDirectory);
    if (!dir.exists())
      return Collections.emptyList();

    try (Stream<Path> walkStream = Files.walk(dir.toPath()))
    {
      return walkStream
          .filter(Files::isRegularFile)
          .filter(pPath -> pPath.toString().toLowerCase().endsWith(".class"))
          .filter(pPath -> !pPath.toString().contains("$"))
          .map(pClassFile -> pClassFile.toAbsolutePath().toString().substring(dir.getAbsolutePath().length() + 1)
              .replace(File.separator, ".")
              .replace(".class", ""))
          .collect(Collectors.toList());
    }
  }

  /**
   * Applies transformations on the class with the given name in the given directory
   *
   * @param pClassPool Pool to load classes via {@link ClassPool}
   * @param pDirectory Root directory, where the given class was loaded from
   * @param pClassName Fully qualified name of the class to load
   * @throws NotFoundException            if the given class could not be found in the given class pool
   * @throws CannotCompileException       if the transformed class is not able to compile anymore
   * @throws IOException                  if we are not able to write to the given directory
   * @throws ReflectiveOperationException if a strategy could not apply some operations via reflection
   */
  private void transformClass(@NonNull ClassPool pClassPool, @NonNull String pDirectory, @NonNull String pClassName)
      throws NotFoundException, CannotCompileException, IOException, ReflectiveOperationException
  {
    // import the package, so we are able to load the class
    pClassPool.importPackage(pClassName);

    // instantiate the class
    CtClass candidateClass = pClassPool.get(pClassName);
    candidateClass.subtypeOf(pClassPool.get(Object.class.getName()));

    // apply the transformation
    for (IModifyBytecodeStrategy strategy : STRATEGIES)
      strategy.applyClassTransformation(candidateClass);

    // special handling: nested classes
    for (CtClass nestedClass : candidateClass.getNestedClasses())
    {
      CtClass nestedCtClass = pClassPool.get(nestedClass.getName());
      nestedCtClass.subtypeOf(pClassPool.get(Object.class.getName()));

      // apply the transformation
      for (IModifyBytecodeStrategy strategy : STRATEGIES)
        strategy.applyClassTransformation(nestedCtClass);

      // write back the class, if necessary
      if(nestedClass.isModified())
        nestedCtClass.writeFile(pDirectory);
    }

    // write back the class, if necessary
    if(candidateClass.isModified())
      candidateClass.writeFile(pDirectory);
  }

}
