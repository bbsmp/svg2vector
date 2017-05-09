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

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import de.vandermeer.execs.options.Abstract_TypedC;
import de.vandermeer.execs.options.Option_SimpleC;
import de.vandermeer.skb.interfaces.application.ApplicationException;
import de.vandermeer.skb.interfaces.messagesets.errors.Templates_OutputDirectory;
import de.vandermeer.skb.interfaces.messagesets.errors.Templates_OutputFile;
import de.vandermeer.svg2vector.applications.core.CliOptionPackage;
import de.vandermeer.svg2vector.applications.core.SvgTargets;

/**
 * Application output options.
 *
 * @author     Sven van der Meer &lt;vdmeer.sven@mykolab.com&gt;
 * @version    v2.1.0-SNAPSHOT build 170420 (20-Apr-17) for Java 1.8
 * @since      v2.1.0
 */
public final class OutputOptions extends CliOptionPackage {

	/** Extensions to remove from an input file name, `svg` and `svgz`. */
	public final static String[] EXTENSION_REMOVALS = new String[]{
			".svg",
			".svgz"
	};

	/** Application option for output file. */
	final private AO_FileOutExt aoFileOut = new AO_FileOutExt();

	/** Application option for output directory. */
	final private AO_DirectoryOutExt aoDirOut = new AO_DirectoryOutExt();

	/** Application option to automatically create output directories. */
	final private AO_CreateDirectories aoCreateDirs = new AO_CreateDirectories();

	/** Application option to automatically overwrite existing files on output. */
	final private AO_OverwriteExisting aoOverwriteExisting = new AO_OverwriteExisting();

	/** Output file name without path elements (no layer mode only). */
	protected Path file;

	/** Output directory path. */
	protected Path directory;

	/** Target file extension. */
	protected String fileExtension;

	/** The pattern for output file names. */
	protected final OutputPattern pattern = new OutputPattern();

	/**
	 * Creates a new option object.
	 */
	public OutputOptions(){
		this.aoDirOut.setDefaultValue(System.getProperty("user.dir"));

		this.setSimpleOptions(
			this.aoCreateDirs,
			this.aoOverwriteExisting
		);

		this.setTypedOptions(
			this.aoFileOut,
			this.aoDirOut
		);
	}

	/**
	 * Returns the settings for creating directories
	 * @return true if set, false otherwise
	 */
	public boolean createDirs(){
		return this.aoCreateDirs.inCli();
	}

	/**
	 * Returns the do-layers flag calculated from the options.
	 * Throws a state exception if output option where not yet set.
	 * @return true if layers can be processed, false otherwise
	 */
	public boolean doLayers(){
		Validate.validState(this.directory!=null);
		return (this.file==null);
	}

	/**
	 * Returns the output directory as path.
	 * Throws a state exception if output option where not yet set.
	 * @return output directory path, null if not set
	 */
	public Path getDirectory(){
		Validate.validState(this.directory!=null);
		return this.directory;
	}

	/**
	 * Returns the output file as path.
	 * Throws a state exception if output option where not yet set.
	 * @return output file path, null if not set
	 */
	public Path getFile(){
		Validate.validState(this.directory!=null);
		return this.file;
	}

	/**
	 * Returns the output file extension.
	 * Throws a state exception if output option where not yet set.
	 * @return output file extension, null if not set
	 */
	public String getFileExtension(){
		Validate.validState(this.directory!=null);
		return this.fileExtension;
	}

	/**
	 * Returns the output pattern.
	 * @return output pattern
	 */
	public OutputPattern getPattern(){
		return this.pattern;
	}

	/**
	 * Returns the pattern as string.
	 * @return pattern as string
	 */
	public String getPatternString(){
		return this.pattern.getPattern().build();
	}

	/**
	 * Checks for warnings and returns a list of warnings if options are set.
	 * Throws a state exception if output option where not yet set.
	 * @return list of warnings, empty if none found, exception if options have not yet been set
	 */
	public List<String> getWarnings(){
		Validate.validState(this.directory!=null);

		List<String> ret = new ArrayList<>();
		if(this.file==null){
			Abstract_TypedC<?>[] options = new Abstract_TypedC<?>[]{
				this.aoFileOut
			};
			for(Abstract_TypedC<?> ao : options){
				if(ao.inCli()){
					ret.add("layers processed but CLI option <" + ao.getCliLong() + "> used, will be ignored");
				}
			}
		}
		else{
			Option_SimpleC[] options = new Option_SimpleC[]{
			};
			for(Option_SimpleC ao : options){
				if(ao.inCli()){
					ret.add("no layers processed but CLI option <" + ao.getCliLong() + "> used, will be ignored");
				}
			}
		}
		return ret;
	}

	/**
	 * Removes options from the output pattern.
	 * @param fout true to remove the output file name (not including the extension)
	 * @param index true if index option should be removed
	 * @param isIndex true if Inkscape index option should be removed
	 * @param isLabel true if Inkscape label option should be removed
	 * @throws ApplicationException if resulting pattern is not valid
	 */
	public void removePatternOptions(boolean fout, boolean index, boolean isIndex, boolean isLabel) throws ApplicationException{
		if(fout){
			pattern.pattern.replaceAll(OutputPattern.FILE_OUT, "");
		}
		if(index){
			pattern.pattern.replaceAll(OutputPattern.LAYER_INDEX, "");
		}
		if(isIndex){
			pattern.pattern.replaceAll(OutputPattern.IS_INDEX, "");
		}
		if(isLabel){
			pattern.pattern.replaceAll(OutputPattern.IS_LABEL, "");
		}
		this.pattern.testPattern();
	}

	/**
	 * Sets the options based on CLI settings and arguments.
	 * If successful
	 * 
	 * * the local directory path points to the output directory (null if none set)
	 * * the local file name will point to the file without file extension, only not in layer mode
	 * * the local file extension will have the target file extension
	 * 
	 * @param doLayers true if options should be set for layer mode, false otherwise
	 * @param target the target, must not be null
	 * @param inFilename the file name of the input file (with path), must not be blank
	 * @throws ApplicationException for any error
	 */
	public void setOptions(boolean doLayers, SvgTargets target, String inFilename) throws ApplicationException{
		Validate.notNull(target);
		Validate.notBlank(inFilename);

		if(doLayers){
			// we do layers, only outDir counts
			Path dirPath = FileSystems.getDefault().getPath(this.aoDirOut.getValue());
			this.testPath(dirPath, false);
			this.testDirectoryContent(dirPath, target);
			this.directory = dirPath;
			this.fileExtension = target.name();
		}
		else{
			// we do not do layers
			Path file = null;
			if(this.aoFileOut.inCli()){
				// first check the output file name
				if(StringUtils.isBlank(this.aoFileOut.getCliValue())){
					throw new ApplicationException(Templates_OutputFile.FN_BLANK, this.getClass().getSimpleName(), "output");
				}
				file = FileSystems.getDefault().getPath(
						StringUtils.substringBeforeLast(this.aoFileOut.getCliValue(), "." + target.name())
				);
			}
			else{
				// use the file name of the input file, including directory information
				String fn = StringUtils.replaceEach(inFilename, EXTENSION_REMOVALS, new String[]{"", ""});
				file = FileSystems.getDefault().getPath(fn);
			}
			// file is set, now a given output directory overwrites the directory set
			if(this.aoDirOut.inCli()){
				Path ao = FileSystems.getDefault().getPath(this.aoDirOut.getCliValue());
				this.testPath(ao, false);
				file = FileSystems.getDefault().getPath(ao.toString(), file.getFileName().toString());
			}

			Validate.validState(file.getNameCount()>0);
			if(file.getNameCount()==1){
				//no path element, test file only with extension
				Path toTest = FileSystems.getDefault().getPath(file.getFileName() + "." + target.name());
				this.testIdentity(inFilename, toTest);
				this.testPath(toTest, true);
			}
			else{
				//path and file, use path and add file with extension then test
				Path toTest = FileSystems.getDefault().getPath(file.getParent().toString(), file.getFileName() + "." + target.name());
				this.testIdentity(inFilename, toTest);
				this.testPath(toTest, true);
			}

			if(file.getNameCount()==1){
				this.file = file;
			}
			else{
				this.directory = file.getParent();
				this.file = file.getFileName();
			}
			this.fileExtension = target.name();
		}
	}

	/**
	 * Sets the base name in the output pattern if not already set.
	 * @param basename the base name, ignored if blank
	 * @throws ApplicationException if resulting pattern is not valid
	 */
	public void setPatternBasename(String basename) throws ApplicationException{
		if(StringUtils.isBlank(basename)){
			return;
		}
		pattern.pattern.replaceAll(OutputPattern.FILE_OUT, basename);
		this.pattern.testPattern();
	}

	/**
	 * Tests a directory for files ending with the target extension.
	 * @param directory the directory, nothing tested if null or does not exist
	 * @param target the target, must not be null
	 * @throws ApplicationException for any error
	 */
	protected final void testDirectoryContent(Path directory, SvgTargets target) throws ApplicationException{
		if(directory==null){
			return;
		}
		Validate.notNull(target);

		File dirFile = directory.toFile();
		if(dirFile.exists()){
			for(File file : dirFile.listFiles()){
				if(file.getName().contains("." + target.name()) && !this.createDirs()){
					throw new ApplicationException(
							Templates_OutputDirectory.DIR_EXIST_NOOVERWRITE,
							this.getClass().getSimpleName(),
							"output",
							directory.toString(),
							target.name(),
							this.aoOverwriteExisting.getCliLong()
					);
				}
			}
		}
	}

	/**
	 * Tests if an input file name is the same as an output file name.
	 * If they are the same, an exception is thrown, nothing happens otherwise
	 * @param in the input file name
	 * @param out the output file as path
	 * @throws ApplicationException if they are the same
	 */
	protected final void testIdentity(String in, Path out) throws ApplicationException{
		Validate.notBlank(in);
		Validate.notNull(out);

		Path pIn = FileSystems.getDefault().getPath(in);
		if(StringUtils.compare(pIn.toString(), out.toString())==0){
			throw new ApplicationException(
					Templates_OutputFile.FN_SAMEAS_INFN,
					this.getClass().getSimpleName(),
					"output",
					pIn.toFile(),
					"input",
					out.toString()
			);
		}
	}

	/**
	 * Tests a path being either a directory or a file name (optional with path information).
	 * @param path the path to test for, must not be null
	 * @param hasFilename flag for testing path as a file name (true) or as a directory (false)
	 * @throws ApplicationException in any error case
	 */
	protected final void testPath(Path path, boolean hasFilename) throws ApplicationException{
		Validate.notNull(path);

		Path testDir = FileSystems.getDefault().getPath(path.toString());
		Path testFile = FileSystems.getDefault().getPath(path.toString());

		if(hasFilename){
			testDir = (path.getNameCount()>1)?FileSystems.getDefault().getPath(path.getParent().toString()):null;
		}
		else{
			testFile = null;
		}

		if(testDir!=null){
			// test directory first
			File dirFile = testDir.toFile();
			if(dirFile.exists()){
				if(!dirFile.isDirectory()){
					throw new ApplicationException(Templates_OutputDirectory.DIR_NOTDIR, this.getClass().getSimpleName(), "output", testDir.toString());
				}
				else if(!dirFile.canWrite()){
					throw new ApplicationException(Templates_OutputDirectory.DIR_CANT_WRITE, this.getClass().getSimpleName(), "output", testDir.toString());
				}
			}
			else{
				if(!this.aoCreateDirs.inCli()){
					throw new ApplicationException(Templates_OutputDirectory.DIR_EXIST_NOOVERWRITE, this.getClass().getSimpleName(), "output", testDir.toString(), this.aoCreateDirs.getCliLong());
				}
			}
		}
		if(testFile!=null){
			// test file next
			File fileFile = testFile.toFile();
			if(fileFile.exists()){
				if(fileFile.isDirectory()){
					throw new ApplicationException(Templates_OutputFile.FILE_IS_DIRECTORY, this.getClass().getSimpleName(), "output", fileFile.toString());
				}
				else if(!this.aoOverwriteExisting.inCli()){
					throw new ApplicationException(
							Templates_OutputFile.FILE_EXIST_NOOVERWRITE,
							this.getClass().getSimpleName(),
							"output",
							fileFile.toString(),
							this.aoOverwriteExisting.getCliLong()
					);
				}
				else if(!fileFile.canWrite()){
					throw new ApplicationException(
							Templates_OutputFile.FILE_CANT_WRITE,
							this.getClass().getSimpleName(),
							"output",
							fileFile.toString()
					);
				}
			}
		}
	}
}
