package org.kt3k.gradle.plugin.coveralls.domain

import org.gradle.api.Project


/**
 * Factory class for SourceReport for Cobertura report file.
 */
class CoberturaSourceReportFactory implements SourceReportFactory {

	@Override
	List<SourceReport> createReportList(Project project, File file) {
		// create parser
		XmlParser parser = new XmlParser()

		// skip external DTD validation
		// see http://xerces.apache.org/xerces2-j/features.html#nonvalidating.load-external-dtd
		parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)

		// parse
		Node coverage = parser.parse(file)

		List<String> sourceDirectories = coverage.sources.source*.text()

		// mapping of [filename] => [hits per line]
		Map<String, Map<Integer, Integer>> hitsPerLineMapForFilename = [:]

		coverage.packages.package.classes.class.each() { cls ->
			// mapping of [line number] => [line hits]
			Map<Integer, Integer> hitsPerLineMap = hitsPerLineMapForFilename.get(cls.'@filename', [:])

			cls.lines.line.each() {
				Integer hits = hitsPerLineMap.get(it.'@number'.toInteger() - 1, 0)
				hitsPerLineMap[it.'@number'.toInteger() - 1] = hits + it.'@hits'.toInteger()
			}
		}

		List<SourceReport> reports = new ArrayList<SourceReport>()

		hitsPerLineMapForFilename.each { String filename, Map<Integer, Integer> cov ->

			// find actual source file from directory candidates
			File sourceFile = actualSourceFile(sourceDirectories, filename)

			if (sourceFile == null) {
				// if sourceFile is not found then ignore the entry
				return
			}

			String source = sourceFile.text

			// create hits per line list
			List<Integer> hitsPerLineList = [null] * source.readLines().size()

			cov.each { Integer line, Integer hits ->
				hitsPerLineList[line] = hits
			}

			reports.add new SourceReport(filename, source, hitsPerLineList)
		}

		return reports

	}

	/**
	 * finds the actual source file path and returns File object
	 *
	 * @param sourceDirs the list of candidate source dirs
	 * @param filename the file name to search
	 * @return found File object
	 */
	private static File actualSourceFile(List<String> sourceDirs, String filename) {
		return sourceDirs.collect { new File(it + '/' + filename) }.find { it.exists() }
	}
}
