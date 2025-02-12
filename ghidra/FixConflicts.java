//replaces .conflict types with what they conflicted with, useful for cleaning up after version tracking
//only replaces 100 types per run, so multiple runs may be needed
//@author Kelly Kinkade <kelly.lynn.martin@gmail.com>
//@category Cleanup
//@keybinding
//@menupath
//@toolbar


import util.CollectionUtils;
import java.util.List;
import java.util.stream.Collectors;
import java.time.Duration;
import java.time.Instant;

import ghidra.app.script.GhidraScript;
import ghidra.program.model.data.*;
import ghidra.util.Swing;


public class FixConflicts extends GhidraScript {

	static int maxPerRun = 1000;

	@Override
	protected void run() throws Exception {
		DataTypeManager dtm = currentProgram.getDataTypeManager();

		int id = dtm.startTransaction("Fixing Conflicts");
		try {
			List<DataType> conflicts = CollectionUtils.asStream(dtm.getAllDataTypes())
				.filter(dt -> dt.getName().contains(".conflict"))
				.collect(Collectors.toList());

			String message = "Fixing " + String.valueOf(conflicts.size()) + " conflicts...";
			println(message);
			Instant start = Instant.now();
			Instant lastSwing = Instant.now();
			monitor.initialize(conflicts.size());
			monitor.setMessage(message);
			for (DataType dt : conflicts) {
				monitor.checkCanceled();
				monitor.setMessage(dt.getName());
				if (dt.isDeleted()) {
					monitor.incrementProgress(1);
					continue;
				}
				String name = dt.getName();
				int i = name.indexOf(".conflict");
				if (i == -1) {
					monitor.incrementProgress(1);
					continue;
				}
				name = name.substring(0, i);
				DataType good = dtm.getDataType(dt.getCategoryPath(), name);
				if (good == null) {
					monitor.incrementProgress(1);
					continue;
				}
				dtm.replaceDataType(dt, good, false);
				monitor.incrementProgress(1);

				Duration swingElapsed = Duration.between(lastSwing, Instant.now());
				if (swingElapsed.toMillis() > 1000)
				{
					Swing.allowSwingToProcessEvents();
					lastSwing = Instant.now();
				}

				Duration elapsed = Duration.between(start, Instant.now());
				if (elapsed.toMillis() > 60000)
				{
					dtm.endTransaction(id, true);
					id = dtm.startTransaction("Fixing Conflicts");
					start = Instant.now();
				}
			}
		} finally {
			dtm.endTransaction(id, true);
		}
	}

}
