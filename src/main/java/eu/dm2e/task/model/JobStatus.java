package eu.dm2e.task.model;

public enum JobStatus {
	NOT_STARTED {
		public String toString() {
			return "NOT_STARTED";
		}
	},
	STARTED {
		public String toString() {
			return "STARTED";
		}
	},
	FINISHED {
		public String toString() {
			return "FINISHED";
		}
	},
	FAILED {
		public String toString() {
			return "FAILED";
		}
	},
}
