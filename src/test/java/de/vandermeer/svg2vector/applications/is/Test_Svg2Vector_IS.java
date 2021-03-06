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

package de.vandermeer.svg2vector.applications.is;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Simple tests for Svg2Vector_IS.
 *
 * @author     Sven van der Meer &lt;vdmeer.sven@mykolab.com&gt;
 * @version    v2.0.0 build 170413 (13-Apr-17) for Java 1.8
 * @since      v2.0.0
 */
public class Test_Svg2Vector_IS {

	/** Prefix for tests that create output. */
	static String OUT_DIR_PREFIX = "target/output-tests/s2v-is/";

	/** Prefix for tests that create output. */
	static String FAKE_EXEC = OUT_DIR_PREFIX + "fake-is-exec";

	/** Standard CLI options for tests. */
	static String[] STD_OPTIONS = new String[]{
			"--create-directories", "--overwrite-existing", "--all-layers", "-q",
			"-x", FAKE_EXEC,
//			"-x", "C:/Program Files/Inkscape/inkscape.exe",
			"--simulate"
	};

	@BeforeClass
	public static void createFakeIsExec(){
		File fake = new File(FAKE_EXEC);
		fake.getParentFile().mkdirs();
		try {
			fake.createNewFile();
		}
		catch (IOException ignore) {}
		fake.setExecutable(true);
	}

	@Test
	public void test_AddedOptions(){
		Svg2Vector_IS app = new Svg2Vector_IS();
		assertEquals(28, app.getAppOptions().length);
	}

	@Test
	public void test_Error_AllMissingOptions(){
		Svg2Vector_IS app = new Svg2Vector_IS();
		String[] args = new String[]{
				""
		};
		assertEquals(-1, app.executeApplication(args));
	}

	@Test
	public void test_Usage(){
		Svg2Vector_IS app = new Svg2Vector_IS();
		String[] args = new String[]{
				"--help"
		};
		assertEquals(1, app.executeApplication(args));
	}

	@Test
	public void test_Version(){
		Svg2Vector_IS app = new Svg2Vector_IS();
		String[] args = new String[]{
				"--version"
		};
		assertEquals(1, app.executeApplication(args));
	}

	@Test
	public void test_TargetHelp(){
		Svg2Vector_IS app = new Svg2Vector_IS();
		String[] args = new String[]{
				"--help",
				"target"
		};
		assertEquals(1, app.executeApplication(args));
	}
}
