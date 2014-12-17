/*
 * The MIT License
 *
 * Copyright 2013 Mirko Friedenhagen.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.AbstractItem;
import hudson.model.Item;
import hudson.model.Node;
import hudson.model.User;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * Defines some helper functions needed by {@link hudson.plugins.jobConfigHistory.JobConfigHistoryJobListener} and
 * {@link hudson.plugins.jobConfigHistory.JobConfigHistorySaveableListener}.
 *
 * @author mfriedenhagen
 */
public class GitHistoryDao implements HistoryDao, ItemListenerHistoryDao, OverviewHistoryDao, NodeListenerHistoryDao {

    /**
     * Our logger.
     */
    private static final Logger LOG = Logger.getLogger(GitHistoryDao.class.getName());
    private final File historyRootDir;
    private final File jenkinsHome;
    private final User currentUser;

    GitHistoryDao(final File historyRootDir, File jenkinsHome, User currentUser) {
        this.historyRootDir = historyRootDir;
        this.jenkinsHome = jenkinsHome;
        this.currentUser = currentUser;
        if (!historyRootDir.exists()) {
            final InitCommand initCommand = Git.init();
            initCommand.setDirectory(historyRootDir);
            try {
                initCommand.call();
            } catch (GitAPIException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void saveItem(AbstractItem item) {
        saveItem(item.getConfigFile());
    }

    @Override
    public void saveNode(Node node) {


    }

    @Override
    public void saveItem(XmlFile file) {
        try {
            final Git gitRepo = Git.open(historyRootDir);
            try {
                final String path = file.getFile().getPath();
                LOG.info("path=" + path);
                final AddCommand addCommand = gitRepo.add();
                addCommand.addFilepattern(path);
                addCommand.call();
                final String user;
                final String userId;
                if (currentUser != null) {
                    user = currentUser.getFullName();
                    userId = currentUser.getId();
                } else {
                    user = "Anonym";
                    userId = Messages.ConfigHistoryListenerHelper_anonymous();
                }
                final CommitCommand commitCommand = gitRepo.commit();
                commitCommand.setAuthor(
                        user,
                        String.format(Locale.ENGLISH, "%s@localhost", userId));
                commitCommand.setCommitter(
                        user,
                        String.format(Locale.ENGLISH, "%s@localhost", userId));
                commitCommand.setMessage(String.format(Locale.ENGLISH, "%s modified by %s@localhost", file, userId));
                commitCommand.call();
            } finally {
                gitRepo.close();
            }
        } catch (WrongRepositoryStateException e) {
            throw new RuntimeException(e);
        } catch (NoMessageException e) {
            throw new RuntimeException(e);
        } catch (UnmergedPathsException e) {
            throw new RuntimeException(e);
        } catch (NoHeadException e) {
            throw new RuntimeException(e);
        } catch (ConcurrentRefUpdateException e) {
            throw new RuntimeException(e);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public SortedMap<String, HistoryDescr> getRevisions(XmlFile xmlFile) {
        return getRevisions(xmlFile.getFile());
    }

    @Override
    public SortedMap<String, HistoryDescr> getRevisions(File configFile) {
        final TreeMap<String, HistoryDescr> map = new TreeMap<String, HistoryDescr>();
        final Git gitRepo;
        try {
            gitRepo = Git.open(historyRootDir);
            try {
                final LogCommand logCommand = gitRepo.log();
                logCommand.addPath(configFile.getPath());

                final Iterable<RevCommit> commits = logCommand.call();
                final Iterator<RevCommit> commitIterator = commits.iterator();
                while (commitIterator.hasNext()) {
                    final RevCommit commit = commitIterator.next();
                    final PersonIdent ident = commit.getAuthorIdent();
                    final int commitTime = commit.getCommitTime();
                    final String format = new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER).format(new Date(commitTime));
                    map.put(format, new HistoryDescr(ident.getName(), ident.getEmailAddress(), "", format));
                }
            } finally {
                gitRepo.close();
            }
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        return map;
    }

    @Override
    public SortedMap<String, HistoryDescr> getRevisions(Node node) {
        return null;
    }

    @Override
    public XmlFile getOldRevision(AbstractItem item, String identifier) {
        return null;
    }

    @Override
    public XmlFile getOldRevision(Node node, String identifier) {
        return null;
    }

    @Override
    public XmlFile getOldRevision(XmlFile xmlFile, String identifier) {
        return null;
    }

    @Override
    public XmlFile getOldRevision(File configFile, String identifier) {
        return null;
    }

    @Override
    public XmlFile getOldRevision(String configFileName, String identifier) {
        return null;
    }

    @Override
    public boolean hasOldRevision(AbstractItem item, String identifier) {
        return false;
    }

    @Override
    public boolean hasOldRevision(Node node, String identifier) {
        return false;
    }

    @Override
    public boolean hasOldRevision(XmlFile xmlFile, String identifier) {
        return false;
    }

    @Override
    public boolean hasOldRevision(File configFile, String identifier) {
        return false;
    }

    @Override
    public void purgeOldEntries(File itemHistoryRoot, int maxEntries) {

    }

    @Override
    public boolean isCreatedEntry(File historyDir) {
        return false;
    }

    @Override
    public void copyHistoryAndDelete(String oldName, String newName) {

    }

    @Override
    public void copyNodeHistoryAndDelete(String oldName, String newName) {

    }

    @Override
    public void createNewItem(Item item) {
        saveItem((AbstractItem) item);
    }

    @Override
    public void renameItem(Item item, String oldName, String newName) {

    }

    @Override
    public void deleteItem(Item item) {

    }

    @Override
    public void createNewNode(Node node) {

    }

    @Override
    public void renameNode(Node node, String oldName, String newName) {

    }

    @Override
    public void deleteNode(Node node) {

    }

    @Override
    public File[] getDeletedJobs(String folderName) {
        return new File[0];
    }

    @Override
    public File[] getDeletedNodes(String folderName) {
        return new File[0];
    }

    @Override
    public File[] getJobs(String folderName) {
        return new File[0];
    }

    @Override
    public File[] getNodes(String folderName) {
        return new File[0];
    }

    @Override
    public File[] getSystemConfigs() {
        return new File[0];
    }

    @Override
    public SortedMap<String, HistoryDescr> getJobHistory(String jobName) {
        return null;
    }

    @Override
    public SortedMap<String, HistoryDescr> getNodeHistory(String nodeName) {
        return null;
    }

    @Override
    public SortedMap<String, HistoryDescr> getSystemHistory(String name) {
        return null;
    }
}
