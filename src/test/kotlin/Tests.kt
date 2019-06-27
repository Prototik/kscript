package kscript.tests

import kscript.app.*
import java.io.*
import kotlin.test.*

/**
 * @author Holger Brandl
 */

class Tests {

    // "comma separated dependencies should be parsed correctly"
    @Test
    fun directiveDependencyCollect() {
        val lines = listOf(
            "//DEPS de.mpicbg.scicomp.joblist:joblist-kotlin:1.1, de.mpicbg.scicomp:kutils:0.7",
            "//DEPS  log4j:log4j:1.2.14"
        )

        val expected = listOf(
            "de.mpicbg.scicomp.joblist:joblist-kotlin:1.1",
            "de.mpicbg.scicomp:kutils:0.7",
            "log4j:log4j:1.2.14"
        )

        assertEquals(expected, Script(lines).collectDependencies())
    }

    @Test
    fun parseAnnotDependencies() {
        val lines =
            listOf("""@file:DependsOn("something:dev-1.1.0-alpha3(T2):1.2.14", "de.mpicbg.scicomp:kutils:0.7")""")

        val expected = listOf(
            "something:dev-1.1.0-alpha3(T2):1.2.14",
            "de.mpicbg.scicomp:kutils:0.7",
            "com.github.holgerbrandl:kscript-annotations:1.4"
        )

        assertEquals(expected, Script(lines).collectDependencies())

        // but reject comma separation within dependency entries
        // note: disabled because quits kscript by design
        //        shouldThrow<IllegalArgumentException> {
        //            extractDependencies("""@file:DependsOn("com.squareup.moshi:moshi:1.5.0,com.squareup.moshi:moshi-adapters:1.5.0")""")
        //        }
    }

    @Test
    fun mixedDependencyCollect() {
        val lines = listOf(
            "//DEPS de.mpicbg.scicomp.joblist:joblist-kotlin:1.1, de.mpicbg.scicomp:kutils:0.7",
            """@file:DependsOn("log4j:log4j:1.2.14")"""
        )

        val expected = listOf(
            "de.mpicbg.scicomp.joblist:joblist-kotlin:1.1",
            "de.mpicbg.scicomp:kutils:0.7",
            "log4j:log4j:1.2.14",
            "com.github.holgerbrandl:kscript-annotations:1.4"
        )

        assertEquals(expected, Script(lines).collectDependencies())
    }


    @Test
    fun customRepo() {
        val lines = listOf(
            """@file:MavenRepository("imagej-releases", "http://maven.imagej.net/content/repositories/releases" ) // crazy comment""",
            """@file:DependsOnMaven("net.clearvolume:cleargl:2.0.1")""",
            """@file:DependsOn("log4j:log4j:1.2.14")""",
            """println("foo")"""
        )

        with(Script(lines)) {
            assertEquals(
                listOf(
                    MavenRepo("imagej-releases", "http://maven.imagej.net/content/repositories/releases")
                ), collectRepos()
            )

            assertEquals(
                listOf(
                    "net.clearvolume:cleargl:2.0.1",
                    "log4j:log4j:1.2.14",
                    "com.github.holgerbrandl:kscript-annotations:1.4"
                ), collectDependencies()
            )
        }

    }

    @Test
    fun customRepoWithCreds() {
        val lines = listOf(
            """@file:MavenRepository("imagej-releases", "http://maven.imagej.net/content/repositories/releases", user="user", password="pass") """,
            // Same but name arg comes last
            """@file:MavenRepository("imagej-snapshots", "http://maven.imagej.net/content/repositories/snapshots", password="pass", user="user") """,
            // Whitespaces around credentials see #228
            """@file:MavenRepository("spaceAroundCredentials", "http://maven.imagej.net/content/repositories/snapshots", password= "pass" , user= "user" ) """,
            // Different whitespaces around credentials see #228
            """@file:MavenRepository("spaceAroundCredentials2", "http://maven.imagej.net/content/repositories/snapshots", password= "pass", user="user" ) """,

            // some other script bits unrelated to the repo definition
            """@file:DependsOnMaven("net.clearvolume:cleargl:2.0.1")""",
            """@file:DependsOn("log4j:log4j:1.2.14")""",
            """println("foo")"""
        )

        with(Script(lines)) {

            assertEquals(
                listOf(
                    MavenRepo(
                        "imagej-releases",
                        "http://maven.imagej.net/content/repositories/releases",
                        "user",
                        "pass"
                    ),
                    MavenRepo(
                        "imagej-snapshots",
                        "http://maven.imagej.net/content/repositories/snapshots",
                        "user",
                        "pass"
                    ),
                    MavenRepo(
                        "spaceAroundCredentials",
                        "http://maven.imagej.net/content/repositories/snapshots",
                        "user",
                        "pass"
                    ),
                    MavenRepo(
                        "spaceAroundCredentials2",
                        "http://maven.imagej.net/content/repositories/snapshots",
                        "user",
                        "pass"
                    )
                ), collectRepos()
            )

            assertEquals(
                listOf(
                    "net.clearvolume:cleargl:2.0.1",
                    "log4j:log4j:1.2.14",
                    "com.github.holgerbrandl:kscript-annotations:1.4"
                ), collectDependencies()
            )
        }

    }


    // combine kotlin opts spread over multiple lines
    @Test
    fun optsCollect() {
        val lines = listOf(
            "//KOTLIN_OPTS -foo 3 'some file.txt'",
            "//KOTLIN_OPTS  --bar"
        )

        assertEquals(listOf("-foo", "3", "some file.txt", "--bar"), Script(lines).collectRuntimeOptions())
    }

    @Test
    fun annotOptsCollect() {
        val lines = listOf(
            "//KOTLIN_OPTS -foo 3 'some file.txt'",
            """@file:KotlinOpts("--bar")"""
        )

        assertEquals(listOf("-foo", "3", "some file.txt", "--bar"), Script(lines).collectRuntimeOptions())
    }

    @Test
    fun detectEntryPoint() {
        assertTrue(isEntryPointDirective("//ENTRY Foo"))
        assertTrue(isEntryPointDirective("""@file:EntryPoint("Foo")"""))

        assertFalse(isEntryPointDirective("""//@file:EntryPoint("Foo")"""))
        assertFalse(isEntryPointDirective("""// //ENTRY Foo"""))


        val commentDriven = """
            // comment
            //ENTRY Foo
            fun a = ""
            """.trimIndent()

        assertEquals(Script(commentDriven.lines()).findEntryPoint(), "Foo")


        val annotDriven = """
            // comment
            @file:EntryPoint("Foo")
            fun a = ""
            """.trimIndent()

        assertEquals(Script(annotDriven.lines()).findEntryPoint(), "Foo")
    }


    @Test
    fun test_consolidate_imports() {
        val file = File("test/resources/consolidate_includes/template.kts")
        val expected = File("test/resources/consolidate_includes/expected.kts")

        val result = resolveIncludes(file)

        assertEquals(expected.readText(), result.scriptFile.readText())
    }


    @Test
    fun test_include_annotations() {
        val file = File("test/resources/includes/include_variations.kts")
        val expected = File("test/resources/includes/expexcted_variations.kts")

        val result = resolveIncludes(file)

        assertEquals(expected.readText(), result.scriptFile.readText())
    }

    @Test
    fun test_include_detection() {
        val result = resolveIncludes(File("test/resources/includes/include_variations.kts"))

        val fileIncludes = result.includes.filter { it.protocol == "file" }
        val nonFileIncludes = result.includes.filter { it.protocol != "file" }

        assertEquals(List(4) { "include_${it + 1}.kt" }, fileIncludes.map { File(it.toURI()).name })
        assertEquals(1, nonFileIncludes.size)
    }

    @Test
    fun `test include detection - should not include dependency twice`() {
        val result = resolveIncludes(File("test/resources/includes/dup_include/dup_include.kts"))

        assertEquals(
            listOf(
                "dup_include_1.kt",
                "dup_include_2.kt"
            ),
            result.includes.map { File(it.toURI()).name }
        )
    }
}
