package in.miskin.graphviz.jgit;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class Main {

	final List<String> specialBranches = Arrays.asList("refs/heads/master",
			"refs/heads/integration");

	private static SimpleDateFormat format = new SimpleDateFormat();

	private static PrintWriter printWriter;

	public void run(final String[] args) throws IOException, GitAPIException {
		final Repository repository;
		repository = openRepository();
		run(repository);
		repository.close();
	}

	/**
	 * @param args
	 * @throws GitAPIException
	 */
	public static void main(final String[] args) throws IOException,
			GitAPIException {
		try {
			printWriter = new PrintWriter(
					"/Users/richard/git/branched_example/java.dot");
			new Main().run(args);
		} finally {
			printWriter.close();
		}
	}

	private void run(final Repository repository) throws GitAPIException,
			IOException {
		final Map<String, Ref> allRefs = repository.getAllRefs();

		final Map<String, CommitNode> allCommits = new HashMap<String, CommitNode>();

		final Git git = new Git(repository);
		final Map<String, CommitNode> branchMap = new HashMap<String, CommitNode>();
		final RevWalk walk = new RevWalk(repository);
		for (final Ref ref : allRefs.values()) {
			if (!ref.isSymbolic() && !ref.getName().startsWith("refs/tags")) {
				final CommitNode branch = walkBranch(allCommits,
						ref.getObjectId(), walk, git);
				branchMap.put(ref.getName(), branch);
			}
		}

		getOutputStream().println("digraph git{");

		// Do the special branches first
		for (final String branchName : specialBranches) {
			final CommitNode commitNode = branchMap.get(branchName);
			if (commitNode != null) {
				printSubgraph(branchName, commitNode);
			}
		}

		// Print the remaining branches
		for (final Entry<String, CommitNode> entry : branchMap.entrySet()) {
			if (!specialBranches.contains(entry.getKey())) {
				printSubgraph(entry.getKey(), entry.getValue());

			}
		}

		final Map<String, Ref> refs = repository.getRefDatabase().getRefs(
				"refs/tags");
		final Map<String, String> idTagMap = new HashMap<String, String>();
		for (final Ref dd : refs.values()) {
			// TODO Could have multiple tags for the same commit.
			idTagMap.put(dd.getObjectId().getName(), dd.getName());
		}

		// TODO Should really just collect nodes as the branches are walked.
		final SortedSet<CommitNode> allCommitsX = new TreeSet<CommitNode>();

		// Collect all of the commits.
		for (final RevCommit o : git.log().all().call()) {
			final CommitNode commitNode = new CommitNode(o);
			if (idTagMap.containsKey(o.getId().getName())) {
				commitNode.addTag(idTagMap.get(o.getId().getName()));
			}
			allCommitsX.add(commitNode);
			if (allCommits.containsKey(commitNode.getId())) {
				final CommitNode linkedNode = allCommits
						.get(commitNode.getId());

			}
		}

		for (final CommitNode node : allCommits.values()) {
			getOutputStream().print("  ");
			getOutputStream().println(node.getNodeString());
		}

		// End the graph
		getOutputStream().println("}");
	}

	private PrintWriter getOutputStream() {
		return printWriter;// System.out;
	}

	private void printSubgraph(final String branchName, final CommitNode branch) {
		// TODO Auto-generated method stub
		getOutputStream().println(
				"subgraph cluster_" + branchName.replace('/', '_') + " {");
		getOutputStream().println("label=\"" + branchName + "\";");
		getOutputStream().println("color=blue;style=dotted;");
		printBranch(branch);
		getOutputStream().println("}");

	}

	/**
	 * Recursively walk down a branch creating BranchElement objects.
	 * 
	 * @param allCommits
	 * @param git
	 * @throws GitAPIException
	 * @throws NoHeadException
	 */
	private CommitNode walkBranch(final Map<String, CommitNode> allCommits,
			ObjectId objectId, final RevWalk walk, final Git git)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException, NoHeadException, GitAPIException {
		CommitNode element = null;
		// RevCommit commit = walk.parseCommit(objectId);

		RevCommit commit = git.log().setMaxCount(1).add(objectId).call()
				.iterator().next();

		try {
			// FIXME Change this to use a map of objectId to CommitNode, only
			// creating objects on the first visit.
			if (allCommits.containsKey(objectId.getName())) {
				element = allCommits.get(objectId.getName());
			} else {
				element = new CommitNode(commit);
				allCommits.put(objectId.getName(), element);
			}

			if (commit.getParentCount() > 0) {
				commit = commit.getParent(0);
				objectId = commit.getId();
				element.addParent(walkBranch(allCommits, objectId, walk, git));
			} else {
				objectId = null;
			}
		} catch (final NullPointerException e) {
			getOutputStream().println("NPE!!");
			commit = null;
		}
		return element;
	}

	private void printBranch(final CommitNode node) {
		for (final CommitNode parent : node.getParents()) {
			getOutputStream().println(
					node.getId() + " -> " + parent.getId() + " [dir=back];");
			printBranch(parent);
		}
	}

	public Repository openRepository() throws IOException {
		final FileRepositoryBuilder builder = new FileRepositoryBuilder();
		final File gitDir = new File("/Users/richard/git/branched_example");
		final Repository repository = builder.readEnvironment()
				.findGitDir(gitDir).addCeilingDirectory(gitDir).build();
		return repository;
	}

}
