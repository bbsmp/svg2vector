/* Copyright 2017 Sven van der Meer <vdmeer.sven@mykolab.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.vandermeer.svg2vector.applications.base.output;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.vandermeer.execs.options.ExecS_CliParser;
import de.vandermeer.svg2vector.ErrorCodes;
import de.vandermeer.svg2vector.S2VExeception;
import de.vandermeer.svg2vector.applications.base.SvgTargets;
import de.vandermeer.svg2vector.applications.base.Test_Artifacts;

/**
 * Tests for {@link OutputOptions} - layer mode.
 *
 * @author     Sven van der Meer &lt;vdmeer.sven@mykolab.com&gt;
 * @version    v2.1.0-SNAPSHOT build 170420 (20-Apr-17) for Java 1.8
 * @since      v2.0.0
 */
public class Test_OutputOptions_Layers {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void testArtifacts(){
		assertTrue(Test_Artifacts.newFile==null);
		assertTrue(Test_Artifacts.newDir==null);
	}

	@After
	public void deleteArtifacts(){
		Test_Artifacts.removeArtifacts();
	}

	@Test
	public void test_Error_DoutNotDir() throws S2VExeception, IOException{
		ExecS_CliParser cli = new ExecS_CliParser();
		OutputOptions oo = new OutputOptions();
		cli.addAllOptions(oo.getOptions());
		String[] args = new String[]{
				"-d", "target/output-tests/app-props/test"
		};

		Test_Artifacts.newFile = new File("target/output-tests/app-props");
		Test_Artifacts.newFile.mkdirs();
		Test_Artifacts.newFile = new File("target/output-tests/app-props/test");
		Test_Artifacts.newFile.createNewFile();

		assertEquals(null, cli.parse(args));
		assertEquals(0, Test_Artifacts.setCli4Options(cli.getCommandLine(), oo.getOptions()));

		thrown.expect(S2VExeception.class);
		thrown.expectMessage("output directory <" + Test_OutputOptions.substPathSeparator("target/output-tests/app-props/test") + "> exists but is not a directory");
		thrown.expect(hasProperty("errorCode", is(ErrorCodes.OUTPUT_DIR_IS_NOT_DIRECTORY__1)));
		oo.setOptions(true, SvgTargets.svg, "foo.svg");
	}

	@Test
	public void test_Error_NoDoutNoCreate() throws S2VExeception{
		ExecS_CliParser cli = new ExecS_CliParser();
		OutputOptions oo = new OutputOptions();
		cli.addAllOptions(oo.getOptions());
		String[] args = new String[]{
				"-d", "target/output-tests/app-props/test/file"
		};

		assertEquals(null, cli.parse(args));
		assertEquals(0, Test_Artifacts.setCli4Options(cli.getCommandLine(), oo.getOptions()));

		thrown.expect(S2VExeception.class);
		thrown.expectMessage("output directory <" + Test_OutputOptions.substPathSeparator("target/output-tests/app-props/test/file") + "> does not exist and CLI option create-directories not used");
		thrown.expect(hasProperty("errorCode", is(ErrorCodes.OUTPUT_DIR_NOTEXISTS_NO_CREATE_DIR_OPTION__2)));
		oo.setOptions(true, SvgTargets.svg, "foo.svg");
	}

	@Test
	public void test_DoesLayers() throws S2VExeception{
		ExecS_CliParser cli = new ExecS_CliParser();
		OutputOptions oo = new OutputOptions();
		cli.addAllOptions(oo.getOptions());
		String[] args = new String[]{
				"-d", "target/output-tests/app-props/",
				"--create-directories"
		};

		Test_Artifacts.newDir = new File("target/output-tests/app-props");

		assertEquals(null, cli.parse(args));
		assertEquals(0, Test_Artifacts.setCli4Options(cli.getCommandLine(), oo.getOptions()));

		oo.setOptions(true, SvgTargets.svg, "foo.svg");
		assertTrue(oo.file==null);
	}

	@Test
	public void test_Warning_Fout() throws S2VExeception{
		ExecS_CliParser cli = new ExecS_CliParser();
		OutputOptions oo = new OutputOptions();
		cli.addAllOptions(oo.getOptions());
		String[] args = new String[]{
				"-d", "target",
				"-o", "foo"
		};

		assertEquals(null, cli.parse(args));
		assertEquals(0, Test_Artifacts.setCli4Options(cli.getCommandLine(), oo.getOptions()));

		oo.setOptions(true, SvgTargets.svg, "foo.svg");
		assertEquals(1, oo.getWarnings().size());
		assertEquals("layers processed but CLI option <output-file> used, will be ignored", oo.getWarnings().get(0));
	}

}