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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import de.vandermeer.svg2vector.applications.base.AppBase;
import de.vandermeer.svg2vector.applications.base.AppProperties;
import de.vandermeer.svg2vector.applications.base.SvgTargets;

/**
 * The Svg2Vector application using an Inkscape executable.
 * It an SVG graphic to a vector format and to PNG as per Inkscape.
 * The tool does support SVG and SVGZ input formats from file or URI.
 *
 * @author     Sven van der Meer &lt;vdmeer.sven@mykolab.com&gt;
 * @version    v2.0.0 build 170413 (13-Apr-17) for Java 1.8
 * @since      v2.0.0
 */
public class Svg2Vector_IS extends AppBase<IsLoader, AppProperties<IsLoader>> {

	/** Application name. */
	public final static String APP_NAME = "s2v-is";

	/** Application display name. */
	public final static String APP_DISPLAY_NAME = "Svg2Vector Inkscape";

	/** Application version, should be same as the version in the class JavaDoc. */
	public final static String APP_VERSION = "v2.0.0 build 170413 (13-Apr-17) for Java 1.8";

	/** Prefix used when creating temporary files or directories. */
	public static final String TMP_FN_PREFIX = "s2vis-";

	/** Application option for the Inkscape executable. */
	AO_InkscapeExecutable optionInkscapeExec = new AO_InkscapeExecutable('x');

	/** Application option to set DPI for bitmap export. */
	AO_ExportDpi optionExpDpi = new AO_ExportDpi(SvgTargets.png, "--export-dpi");

	/** Application option to set PS level for PS export. */
	AO_ExportPsLevel optionExpPslevel = new AO_ExportPsLevel(SvgTargets.ps, "--export-ps-level");

	/** Application option to set PDF version for PDF export. */
	AO_ExportPdfVersion optionExpPdfver = new AO_ExportPdfVersion(SvgTargets.pdf, "--export-pdf-version");

	/** Application option to require SVG transformation first, then do the actual target transformation. */
	AO_SvgFirst optionSvgFirst = new AO_SvgFirst(false, 'g', "requires the tool to generate temporary SVG files first and then us those files to generate the actual target");

	/** Application option to manage layers manually when creating a temporary directory. */
	AO_ManualLayers optionManualLayers = new AO_ManualLayers(false, 'm', "layers are switched off/on on a raw text file, i.e. not using any SVG or XML library");

	/** Path object for the temporary directory. */
	Path tmpDir;

	/** File for a temporary created SVG file. */
	Path tmpFile;

	/**
	 * Returns a new application.
	 */
	public Svg2Vector_IS(){
		super(new AppProperties<IsLoader>(SvgTargets.values(), new IsLoader()));

		this.addOption(this.optionExpDpi);
		this.addOption(this.optionExpPdfver);
		this.addOption(this.optionExpPslevel);
		this.addOption(this.optionSvgFirst);
		this.addOption(this.optionManualLayers);

		this.addOption(this.optionInkscapeExec);
	}

	/**
	 * Checks the Inkscape executable.
	 * @param fn the file name of the executable
	 * @return 0 on success, negative integer on error with error messages printed
	 */
	private int checkIsExecutable(String fn){
		if(StringUtils.isBlank(fn)){
			this.printErrorMessage("expected Inkscape executable, found <" + fn + ">");
			return -20;
		}
		File testFD = new File(fn);
		if(!testFD.exists()){
			this.printErrorMessage("Inkscape executable <" + fn + "> does not exist, please check path and filename");
			return -21;
		}
		if(!testFD.isFile()){
			this.printErrorMessage("Inkscape executable <" + fn + "> is not a file, please check path and filename");
			return -22;
		}
		if(!testFD.canExecute()){
			this.printErrorMessage("cannot execute input Inkscape executable <" + fn + ">, please file permissions");
			return -23;
		}
		return 0;
	}

	/**
	 * Converts the input (or temporary artifact) into the target format.
	 * @param isCmd the command for creating them
	 * @parameter target the target, must not be null
	 * @return 0 on success, negative integer on error with error messages printed
	 */
	private int convertInput(IsCmd isCmd, SvgTargets target){
		int ret = 0;

		if(this.optionSvgFirst.inCli() && this.getProps().doesLayers()){
			this.printProgressMessage("converting multiple temporary SVG files");

			if(this.getProps().canWriteFiles()){
				//the real deal, process files
				//there should be tmpDir, if not it's an error
				if(this.tmpDir==null && this.getProps().canWriteFiles()){
					this.printErrorMessage("implementation error: expected tmp dir to exist, but was null");
					return -92;
				}
				for (final File fileEntry : this.tmpDir.toFile().listFiles()) {
					if(fileEntry.isFile()){
						String finTmp = this.tmpDir + "/" + fileEntry.getName();
						String fout = this.getProps().getDout() + "/" + StringUtils.substringBefore(fileEntry.getName(), ".svg") + "." + target.name();
						this.ExecInkscape(isCmd, finTmp, fout);
					}
				}
			}
			else{
				//simulation, only some messages
				this.printDetailMessage("would create target files, 1 per layer now, from temporary files");
			}
		}
		else if(this.optionSvgFirst.inCli()){
			this.printProgressMessage("converting single temporary SVG file");

			//there should be a tmp file, if not it's an error if we can write files
			if(this.tmpFile==null && this.getProps().canWriteFiles()){
				this.printErrorMessage("implementation error: expected tmp file to exist, but was null");
				return -93;
			}
			this.ExecInkscape(isCmd, (this.getProps().canWriteFiles())?this.tmpFile.toString():TMP_FN_PREFIX, this.getProps().getFoutFn());
		}
		else{
			//no tmp dir/file created, to a conversion from source to target
			if(this.getProps().doesLayers()){
				//for multi layers
				for(Entry<String, Integer> entry : this.getProps().getLoader().getLayers().entrySet()){
					String fout = this.getProps().getFnOut(entry) + "." + target.name();
					String nodeId = "layer" + entry.getValue().toString();
					IsCmd nodeCmd = new IsCmd(isCmd);//TODO was isTmpCmd
					nodeCmd.appendSelectedNode(nodeId);
					this.ExecInkscape(nodeCmd, this.getProps().getFinFn(), fout);
					if(ret<0){
						return ret;
					}
				}
			}
			else{
				//for single file, no layer processing
				this.ExecInkscape(isCmd, this.getProps().getFinFn(), this.getProps().getFoutFn());
			}
		}

		return ret;
	}

	/**
	 * Creates temporary artifacts, if requested.
	 * @param isTmpCmd the command for creating them
	 * @return 0 on success, negative integer on error with error messages printed
	 */
	private int createTempArtifacts(IsCmd isTmpCmd){
		int ret = 0;
		IsLoader loader = this.getProps().getLoader();

		if(this.optionSvgFirst.inCli()){
			if(this.getProps().doesLayers()){
				this.printProgressMessage("creating temporary directory");
				if(this.getProps().canWriteFiles()){
					try{
						this.tmpDir = Files.createTempDirectory(TMP_FN_PREFIX);
					}
					catch (IOException e) {
						this.printErrorMessage("problem creating temporary directory with error: " + e.getMessage());
						return -90;
					}
					this.printDetailMessage("temp directory:   " + this.tmpDir);
				}
				else{
					this.printDetailMessage("temp dir prefix:  " + TMP_FN_PREFIX);
				}

				this.printProgressMessage("creating temporary SVG files");
				if(this.optionManualLayers.inCli()){
					this.printDetailMessage("using manual layer handling");
					for(Entry<String, Integer> entry : loader.getLayers().entrySet()){
						loader.switchOffAllLayers();
						loader.switchOnLayer(entry.getKey());
						String err = this.write(((this.getProps().canWriteFiles())?this.tmpDir.toString():TMP_FN_PREFIX) + "/" + this.getProps().getFnOutNoDir(entry) + ".svg", loader.getLines());
						if(err!=null){
							this.printErrorMessage(err);
							return -92;
						}
					}
				}
				else{
					this.printDetailMessage("using Inkscape for layer handling");
					for(Entry<String, Integer> entry : loader.getLayers().entrySet()){
						String fout = this.tmpDir.toString() + "/" + this.getProps().getFnOutNoDir(entry) + ".svg";
						String nodeId = "layer" + entry.getValue().toString();
						IsCmd nodeCmd = new IsCmd(isTmpCmd);
						nodeCmd.appendSelectedNode(nodeId);
						ret = this.ExecInkscape(nodeCmd, this.getProps().getFinFn(), fout);
						if(ret<0){
							return ret;
						}
					}
				}
			}
			else{
				this.printProgressMessage("creating temporary file");
				if(this.getProps().canWriteFiles()){
					try{
						this.tmpFile = Files.createTempFile(TMP_FN_PREFIX, null);
					}
					catch (IOException e) {
						this.printErrorMessage("problem creating temporary file with error: " + e.getMessage());
						return -91;
					}
					this.printDetailMessage("temp file:        " + tmpFile);
				}
				else{
					this.printDetailMessage("temp file prefix: " + TMP_FN_PREFIX);
				}

				ret = this.ExecInkscape(isTmpCmd, this.getProps().getFinFn(), (this.getProps().canWriteFiles())?this.tmpFile.toString():TMP_FN_PREFIX);
				if(ret<0){
					return ret;
				}
			}
		}
		return ret;
	}

	public int ExecInkscape(IsCmd cmd, String fin, String fout){
		String cli = cmd.substitute(fin, fout);

		if(this.getProps().canWriteFiles()){
			try {
				Process p = Runtime.getRuntime().exec(cli);
				p.waitFor();
			}
			catch (IOException e) {
				this.printErrorMessage("IO exception while executing Inkscape with error: " + e.getMessage());
				return -110;
			}
			catch (InterruptedException e) {
				this.printErrorMessage("InterruptedException exception while executing Inkscape with error: " + e.getMessage());
				return -111;
			}
		}

		this.printDetailMessage("");
		this.printDetailMessage("running IS for input <" + fin + "> creating output <" + fout + ">");
		this.printDetailMessage("running IS with cli <" + cli + ">");
		this.printDetailMessage("");
		return 0;
	}

	@Override
	public int executeApplication(String[] args) {
		int ret = super.executeApplication(args);
		if(ret!=0){
			return ret;
		}

		SvgTargets target = this.getProps().getTarget();

		String fn = this.optionInkscapeExec.getValue();
		if((ret = this.checkIsExecutable(fn))<0){
			return ret;
		}
		this.printDetailMessage("Inkscape exec:    " + fn);

		this.setWarnings(target);

		IsCmd isCmd = new IsCmd(fn, target, this.getProps());
		isCmd.appendTargetSettings(target,
				this.optionExpDpi, this.optionExpPdfver, this.optionExpPslevel
		);
		IsCmd isTmpCmd = new IsCmd(fn, SvgTargets.svg, this.getProps());

		if(this.optionSvgFirst.inCli()){
			this.printProgressMessage("converting to temporary SVG first");
			this.printDetailMessage("Inkscape cmd tmp: " + isTmpCmd);
		}
		else{
			this.printProgressMessage("converting directly to target");
			this.printDetailMessage("Inkscape cmd:     " + isCmd);
		}

		ret = this.createTempArtifacts(isTmpCmd);
		if(ret<0){
			return ret;
		}

		ret = this.convertInput(isCmd, target);
		if(ret<0){
			return ret;
		}

		this.removeTempArtifacts();
		this.printProgressMessage("finished successfully");
		return 0;
	}

	@Override
	public String getAppDescription() {
		return "Converts SVG graphics into other vector formats using Inkscape, with options for handling layers";
	}

	@Override
	public String getAppDisplayName(){
		return APP_DISPLAY_NAME;
	}

	@Override
	public String getAppName() {
		return APP_NAME;
	}

	@Override
	public String getAppVersion() {
		return APP_VERSION;
	}

	/**
	 * Removes temporary artifacts (files and directories).
	 */
	private void removeTempArtifacts(){
		if(!this.getProps().doesKeepTempArtifacts()){
			this.printProgressMessage("removing temporary artifacts");
			if(this.tmpDir!=null){
				for (final File fileEntry : this.tmpDir.toFile().listFiles()) {
					fileEntry.delete();
				}
				this.tmpDir.toFile().delete();
			}
			if(this.tmpFile!=null){
				this.tmpFile.toFile().delete();
			}
		}
	}

	/**
	 * Checks for all CLI options and target and creates warnings if necessary.
	 * @param target the target, should not be null
	 * @throws NullPointerException if target was null
	 */
	private void setWarnings(SvgTargets target){
		Validate.notNull(target);

		if(target!=SvgTargets.pdf && this.optionExpPdfver.inCli()){
			this.getProps().getWarnings().add("target is not <pdf> but CLI option <" + this.optionExpPdfver.getCliOption().getLongOpt() + "> used, will be ignored");
		}
		if(target!=SvgTargets.png && this.optionExpDpi.inCli()){
			this.getProps().getWarnings().add("target is not <png> but CLI option <" + this.optionExpDpi.getCliOption().getLongOpt() + "> used, will be ignored");
		}
		if(target!=SvgTargets.ps && this.optionExpPslevel.inCli()){
			this.getProps().getWarnings().add("target is not <ps> but CLI option <" + this.optionExpPslevel.getCliOption().getLongOpt() + "> used, will be ignored");
		}
		if(!this.optionSvgFirst.inCli() && this.optionManualLayers.inCli()){
			this.getProps().getWarnings().add("found CLI option <" + this.optionManualLayers.getCliOption().getLongOpt() + "> but not <" + this.optionSvgFirst.getCliOption().getLongOpt() + ">, option will be ignored");
		}
		if(this.getProps().doesLayers()){
			
		}
		if(this.getProps().doesNoLayers()){
			if(this.optionManualLayers.inCli()){
				this.getProps().getWarnings().add("no layers processed but CLI option <" + this.optionManualLayers.getCliOption().getLongOpt() + "> used, will be ignored");
			}
		}
		this.printWarnings();
	}

	/**
	 * Writes lines of an list to a file.
	 * @param fn the name of the file
	 * @param lines the lines to write to the file
	 * @return null on success, error message on error
	 */
	public String write(String fn, ArrayList<String> lines){
		if(StringUtils.isBlank(fn)){
			return "write: file name was blank";
		}
		if(lines==null){
			return "write: lines was null";
		}
		if(lines.size()==0){
			return "write: size of lines was 0";
		}

		if(this.getProps().canWriteFiles()){
			FileWriter writer;
			try {
				writer = new FileWriter(fn);
			}
			catch (IOException e) {
				return "IO error creating file writer: " + e.getMessage();
			} 

			try {
				for(String str: lines) {
					writer.write(str);
				}
				writer.close();
			}
			catch (IOException e) {
				return "IO error writing to file <" + fn + "> or closing writer: " + e.getMessage();
			}
		}

		this.printDetailMessage("temporary file: " + fn);
		return null;
	}

}
