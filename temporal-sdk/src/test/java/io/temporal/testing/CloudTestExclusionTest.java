package io.temporal.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import io.temporal.testing.CloudTestExclusion.NeedsCloudAdaptation;
import io.temporal.testing.CloudTestExclusion.RequiresCloudProvisioning;
import io.temporal.testing.CloudTestExclusion.RequiresLocalServer;
import java.io.File;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.experimental.categories.Categories.CategoryFilter;
import org.junit.experimental.categories.Category;
import org.junit.runner.Description;

public class CloudTestExclusionTest {
  private static final Set<Class<?>> REASONS =
      new HashSet<>(
          Arrays.asList(
              RequiresLocalServer.class,
              RequiresCloudProvisioning.class,
              NeedsCloudAdaptation.class));

  @Test
  public void cloudTestExclusionContract() throws Exception {
    assertUmbrellaCategoryExcludesEveryReason();
    assertClassLevelMetadataIsInherited();
    assertEveryCloudExclusionHasOneReasonAndNote();
    assertInvalidCloudExclusionMetadataIsRejected();
  }

  private static void assertUmbrellaCategoryExcludesEveryReason() throws Exception {
    CategoryFilter filter = CategoryFilter.exclude(CloudTestExclusion.class);

    assertFalse(
        filter.shouldRun(
            Description.createSuiteDescription(
                ClassExcludedFixture.class.getName(),
                ClassExcludedFixture.class.getAnnotations())));
    assertFalse(filter.shouldRun(methodDescription("requiresCloudProvisioning")));
    assertFalse(filter.shouldRun(methodDescription("needsCloudAdaptation")));
    assertTrue(filter.shouldRun(methodDescription("cloudEligible")));
  }

  private static void assertEveryCloudExclusionHasOneReasonAndNote() throws Exception {
    for (Class<?> testClass : testClasses()) {
      validateElement(testClass.getName(), testClass);
      for (Method method : testClass.getDeclaredMethods()) {
        validateElement(testClass.getName() + "#" + method.getName(), method);
      }
    }
  }

  private static void assertInvalidCloudExclusionMetadataIsRejected() {
    assertInvalid(MissingNoteFixture.class);
    assertInvalid(OrphanNoteFixture.class);
    assertInvalid(MultipleReasonsFixture.class);
    assertInvalid(UnknownReasonFixture.class);
    assertInvalid(BlankNoteFixture.class);
    assertInvalid(PaddedNoteFixture.class);
    assertInvalid(UncapitalizedNoteFixture.class);
    assertInvalid(NonSentenceNoteFixture.class);
  }

  private static List<Class<?>> testClasses() throws Exception {
    Path classesRoot =
        Paths.get(
            CloudTestExclusionTest.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
    Path packageRoot = classesRoot.resolve("io/temporal");
    try (Stream<Path> paths = Files.walk(packageRoot)) {
      return paths
          .filter(path -> path.toString().endsWith(".class"))
          .map(classesRoot::relativize)
          .map(Path::toString)
          .filter(classFile -> !classFile.contains("CloudTestExclusionTest$"))
          .sorted()
          .map(CloudTestExclusionTest::loadClass)
          .collect(Collectors.toList());
    }
  }

  private static Class<?> loadClass(String classFile) {
    String className =
        classFile
            .substring(0, classFile.length() - ".class".length())
            .replace(File.separatorChar, '.');
    try {
      return Class.forName(className, false, CloudTestExclusionTest.class.getClassLoader());
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Unable to inspect test class " + className + ".", e);
    }
  }

  private static void validateElement(String name, AnnotatedElement element) {
    Category category = element.getAnnotation(Category.class);
    CloudTestExclusionNote note = element.getAnnotation(CloudTestExclusionNote.class);
    List<Class<?>> reasons =
        category == null
            ? java.util.Collections.emptyList()
            : Arrays.stream(category.value())
                .filter(CloudTestExclusion.class::isAssignableFrom)
                .collect(Collectors.toList());
    if (reasons.isEmpty()) {
      assertNull(name + " has a Cloud exclusion note without a reason category.", note);
      return;
    }

    assertEquals(name + " must have exactly one Cloud exclusion reason.", 1, reasons.size());
    assertTrue(name + " uses an unknown Cloud exclusion reason.", REASONS.contains(reasons.get(0)));
    assertNotNull(name + " must explain its Cloud exclusion.", note);
    validateNote(name, note.value());
  }

  private static void validateNote(String name, String note) {
    assertEquals(name + " must not pad its Cloud exclusion note.", note.trim(), note);
    assertFalse(name + " must have a nonblank Cloud exclusion note.", note.isEmpty());
    assertTrue(
        name + " must capitalize its Cloud exclusion note.",
        Character.isUpperCase(note.codePointAt(0)));
    char finalCharacter = note.charAt(note.length() - 1);
    assertTrue(
        name + " must use a sentence for its Cloud exclusion note.",
        finalCharacter == '.' || finalCharacter == '?' || finalCharacter == '!');
  }

  private static Description methodDescription(String name) throws Exception {
    Method method = MethodExcludedFixture.class.getDeclaredMethod(name);
    return Description.createTestDescription(
        MethodExcludedFixture.class, name, method.getAnnotations());
  }

  private static void assertClassLevelMetadataIsInherited() {
    Category category = InheritedChildFixture.class.getAnnotation(Category.class);
    CloudTestExclusionNote note =
        InheritedChildFixture.class.getAnnotation(CloudTestExclusionNote.class);
    assertNotNull(category);
    assertTrue(Arrays.asList(category.value()).contains(RequiresLocalServer.class));
    assertNotNull(note);
    assertEquals("Provides inherited exclusion metadata.", note.value());
    validateElement(InheritedChildFixture.class.getName(), InheritedChildFixture.class);
  }

  private static void assertInvalid(Class<?> fixture) {
    assertThrows(AssertionError.class, () -> validateElement(fixture.getName(), fixture));
  }

  @CloudTestExclusionNote("Requires a local server for this filtering fixture.")
  @Category(RequiresLocalServer.class)
  private static class ClassExcludedFixture {}

  @CloudTestExclusionNote("Provides inherited exclusion metadata.")
  @Category(RequiresLocalServer.class)
  private static class InheritedBaseFixture {}

  private static class InheritedChildFixture extends InheritedBaseFixture {}

  private static class MethodExcludedFixture {
    public void cloudEligible() {}

    @CloudTestExclusionNote("Requires Cloud provisioning for this filtering fixture.")
    @Category(RequiresCloudProvisioning.class)
    public void requiresCloudProvisioning() {}

    @CloudTestExclusionNote("Needs Cloud adaptation for this filtering fixture.")
    @Category(NeedsCloudAdaptation.class)
    public void needsCloudAdaptation() {}
  }

  @Category(RequiresLocalServer.class)
  private static class MissingNoteFixture {}

  @CloudTestExclusionNote("Has a note but no reason category.")
  private static class OrphanNoteFixture {}

  @CloudTestExclusionNote("Has more than one reason category.")
  @Category({RequiresLocalServer.class, NeedsCloudAdaptation.class})
  private static class MultipleReasonsFixture {}

  private interface UnknownReason extends CloudTestExclusion {}

  @CloudTestExclusionNote("Uses a reason outside the supported taxonomy.")
  @Category(UnknownReason.class)
  private static class UnknownReasonFixture {}

  @CloudTestExclusionNote("")
  @Category(RequiresLocalServer.class)
  private static class BlankNoteFixture {}

  @CloudTestExclusionNote(" Padded note.")
  @Category(RequiresLocalServer.class)
  private static class PaddedNoteFixture {}

  @CloudTestExclusionNote("not a capitalized sentence.")
  @Category(RequiresLocalServer.class)
  private static class UncapitalizedNoteFixture {}

  @CloudTestExclusionNote("Not a sentence")
  @Category(RequiresLocalServer.class)
  private static class NonSentenceNoteFixture {}
}
