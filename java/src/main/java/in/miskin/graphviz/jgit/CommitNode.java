package in.miskin.graphviz.jgit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jgit.revwalk.RevCommit;

/** */
class CommitNode implements Comparable<CommitNode> {
	private final String id;
	private final List<CommitNode> parents = new ArrayList<CommitNode>();
	private final List<CommitNode> children = new ArrayList<CommitNode>();
	private final Set<String> tags = new TreeSet<String>();
	private final long timestamp;
	private final String message;

	CommitNode(final String id, final String message, final int commitTime) {
		this.id = id;
		this.message = message;
		this.timestamp = commitTime * 1000;
	}

	CommitNode(final RevCommit commit) {
		this(commit.getId().getName(), commit.getShortMessage(), commit
				.getCommitTime());
	}

	public String getId() {
		return "\"" + id + "\"";
	}

	public void addParent(final CommitNode element) {
		if (element != null) {
			parents.add(element);
			element.addChild(this);
		}
	}

	private void addChild(final CommitNode commitNode) {
		children.add(commitNode);
	}

	public void addTag(final String tag) {
		if (tag != null) {
			tags.add(tag);
		}
	}

	public List<CommitNode> getParents() {
		return Collections.unmodifiableList(parents);
	}

	public String getMessage() {
		return message;
	}

	public String getNodeString() {
		final StringBuilder builder = new StringBuilder("\"");
		builder.append(id);
		builder.append("\"   ");
		builder.append("[label=\"");
		builder.append(formatCommitMessage());
		for (final Iterator<String> iterator = tags.iterator(); iterator
				.hasNext();) {
			final String tag = iterator.next();
			builder.append(tag);
			if (iterator.hasNext()) {
				builder.append(", ");
			}
		}
		if (children.isEmpty()) {
			builder.append("{TIP}");
		}
		builder.append("\", shape=box, style=filled, color=black, fillcolor=white]");
		return builder.toString();
	}

	public int compareTo(final CommitNode o) {
		if (this.timestamp < o.timestamp) {
			return -1;
		} else if (this.timestamp > timestamp) {
			return 1;
		}
		return 0;
	}

	private String formatCommitMessage() {
		// Replace characters that cause problems in the generated dot file.
		final String msg = message.replaceAll("\"", "'")
				.replaceAll("\\\\", "/");
		System.out.println("message:" + message);
		System.out.println("msg    :" + msg);
		return msg;
	}

	public static void main(final String[] args) {
		new CommitNode("", "abc", 0).formatCommitMessage();
		new CommitNode("", "a\"bc", 0).formatCommitMessage();
		new CommitNode("", "a\\bc", 0).formatCommitMessage();

	}

}