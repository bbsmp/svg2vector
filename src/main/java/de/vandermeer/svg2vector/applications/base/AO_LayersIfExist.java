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

package de.vandermeer.svg2vector.applications.base;

import org.apache.commons.cli.Option;

import de.vandermeer.execs.options.AbstractApplicationOption;

/**
 * Application option `layers-if-exist`.
 *
 * @author     Sven van der Meer &lt;vdmeer.sven@mykolab.com&gt;
 * @version    v2.0.0 build 170413 (13-Apr-17) for Java 1.8
 * @since      v2.0.0
 */
public class AO_LayersIfExist extends AbstractApplicationOption<String> {

	/**
	 * Returns the new option.
	 */
	public AO_LayersIfExist(){
		super("process layers if if input file has layers, create one file per layer", "Create one output file (for given target) file per Inkscape layer. Input files without layers will be processed normally. This option might result in the input file being openend and read more than once.");

		Option.Builder builder = Option.builder("L");
		builder.longOpt("layers-if-exist");
		builder.required(false);
		this.setCliOption(builder.build());
	}

	@Override
	public String convertValue(Object value) {
		if(value==null){
			return null;
		}
		return value.toString();
	}

}
