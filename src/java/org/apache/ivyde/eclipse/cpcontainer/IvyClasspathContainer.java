package org.apache.ivyde.eclipse.cpcontainer;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.IvyListener;
import org.apache.ivy.core.event.download.EndArtifactDownloadEvent;
import org.apache.ivy.core.event.download.PrepareDownloadEvent;
import org.apache.ivy.core.event.download.StartArtifactDownloadEvent;
import org.apache.ivy.core.event.resolve.EndResolveDependencyEvent;
import org.apache.ivy.core.event.resolve.StartResolveDependencyEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.report.XmlReportOutputter;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.plugins.repository.TransferListener;
import org.apache.ivy.util.Message;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ClasspathAttribute;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.swt.widgets.Display;


/**
 *   
 * 
 */
public class IvyClasspathContainer implements IClasspathContainer {

    private final class IvyResolveJob extends Job implements TransferListener, IvyListener {
        long _expectedTotalLength = 1;
        long _currentLength = 0;

        private IProgressMonitor _monitor;
        private IProgressMonitor _dlmonitor;
        private Ivy _ivy;
        private CacheManager _cacheMgr;
        private boolean _usePreviousResolveIfExist;
        private int _workPerArtifact = 100;
        private boolean _notify;

        public IvyResolveJob(Ivy ivy, boolean usePreviousResolveIfExist, boolean notify) {
        	super("Resolve "+_javaProject.getProject().getName()+"/"+_ivyXmlPath+" dependencies");
        	_ivy = ivy;
        	_cacheMgr = CacheManager.getInstance(_ivy.getSettings());
        	_usePreviousResolveIfExist = usePreviousResolveIfExist;
        	_notify = notify;
        }

        public void transferProgress(TransferEvent evt) {
        	switch (evt.getEventType()) {
        	case TransferEvent.TRANSFER_INITIATED:
        		_monitor.setTaskName("downloading "+evt.getResource());
        		break;
        	case TransferEvent.TRANSFER_STARTED:
        		_currentLength = 0;
        		if (evt.isTotalLengthSet()) {
        			_expectedTotalLength = evt.getTotalLength();
        			_dlmonitor.beginTask("downloading "+evt.getResource(), 100);
        		}
        		break;
        	case TransferEvent.TRANSFER_PROGRESS:
        		if (_expectedTotalLength > 1) {
        			_currentLength += evt.getLength();
        			_dlmonitor.worked((int)(_currentLength * 100 / _expectedTotalLength));
        			_monitor.subTask((_currentLength/1024)+" / "+(_expectedTotalLength/1024)+"kB");
        		}
        		break;
        	default:
        	}
        }

        public void progress(IvyEvent event) {
        	if (event instanceof TransferEvent) {
        		if (_dlmonitor != null) {
        			transferProgress((TransferEvent)event);
        		}
        	} else if (event instanceof PrepareDownloadEvent) {
        		PrepareDownloadEvent pde = (PrepareDownloadEvent)event;                    
        		Artifact[] artifacts = pde.getArtifacts();
        		if (artifacts.length > 0) {
        			_workPerArtifact = 1000 / artifacts.length;
        		}
        	} else if (event instanceof StartArtifactDownloadEvent) {
        		StartArtifactDownloadEvent evt = (StartArtifactDownloadEvent)event;
        		_monitor.setTaskName("downloading "+evt.getArtifact());
        		if (_dlmonitor != null) {
        			_dlmonitor.done();
        		}
        		_dlmonitor = new SubProgressMonitor(_monitor, _workPerArtifact);
        	} else if (event instanceof EndArtifactDownloadEvent) {
        		if(_dlmonitor != null) {
        			_dlmonitor.done();
        		}
        		_monitor.subTask(" ");
        		_dlmonitor = null;
        	} else if (event instanceof StartResolveDependencyEvent) {
        		StartResolveDependencyEvent ev = (StartResolveDependencyEvent) event;
        		_monitor.subTask("resolving "+ev.getDependencyDescriptor().getDependencyRevisionId());
        	} else if (event instanceof EndResolveDependencyEvent) {
        		_monitor.subTask(" ");
        	}
        }

        protected IStatus run(IProgressMonitor monitor) {
            Message.info("resolving dependencies of "+_ivyXmlFile); 
        	_monitor = monitor;
        	final IStatus[] status = new IStatus[1];
        	final ClasspathItem[][] classpathItems = new ClasspathItem[1][];

        	Thread resolver = new Thread() {
        		public void run() {
        			IvyPlugin.setIvyContext(_javaProject);
        			_ivy.getEventManager().addIvyListener(IvyResolveJob.this);

        			_monitor.beginTask("resolving dependencies", 1000);
					_monitor.setTaskName("resolving dependencies...");
        			//context Classloader hook for commonlogging used by httpclient  
        			ClassLoader old = Thread.currentThread().getContextClassLoader();
        			List problemMessages = Collections.EMPTY_LIST;
        			ModuleDescriptor md = null;
        			try {
        				Thread.currentThread().setContextClassLoader(IvyClasspathContainer.class.getClassLoader());
        				final URL ivyURL = _ivyXmlFile.toURL();
        				String[] confs;
        				boolean resolved = false;
        				try {

        					if (_usePreviousResolveIfExist) {
        						md = ModuleDescriptorParserRegistry.getInstance().parseDescriptor(_ivy.getSettings(), ivyURL, false);
        						if (_confs.length == 1 && "*".equals(_confs[0])) {
        							confs = md.getConfigurationsNames();
        						} else {
        							confs = _confs;
        						}

        						// we check if all required configurations have been resolved
        						for (int i = 0; i < confs.length; i++) {
        							File report = 
        									_cacheMgr
        									.getConfigurationResolveReportInCache(
        											ResolveOptions.getDefaultResolveId(md), 
        											confs[i]);
        							if (!report.exists()) {
        								// no resolve previously done for at least one conf... we do it now
        								Message.info("\n\nIVY DE: previous resolve of " + md.getModuleRevisionId().getModuleId() + " doesn't contain enough data: resolving again\n");
        								ResolveReport r = _ivy.resolve(ivyURL, new ResolveOptions().setConfs(_confs));
        								resolved = true;
        								confs = r.getConfigurations();
                						//eventually do a retrieve
                						if(IvyPlugin.shouldDoRetrieve(_javaProject)) {
                							_monitor.setTaskName("retrieving dependencies in "+IvyPlugin.getFullRetrievePatternHerited(_javaProject));
                							_ivy.retrieve(
                									md.getModuleRevisionId(), 
                									IvyPlugin.getFullRetrievePatternHerited(_javaProject), 
                									new RetrieveOptions().setConfs(confs));
                						}
        								break;
        							}
        						}
        					} else {
        						Message.info("\n\nIVYDE: calling resolve on " + ivyURL + "\n");
        						ResolveReport report = _ivy.resolve(ivyURL, new ResolveOptions().setConfs(_confs));
        						problemMessages = report.getAllProblemMessages();
        						confs = report.getConfigurations();
        						md = report.getModuleDescriptor();
        						resolved = true;

        						if (_monitor.isCanceled()) {
        							status[0] = Status.CANCEL_STATUS;
        							return;
        						}
        						// call retrieve if required
        						if(IvyPlugin.shouldDoRetrieve(_javaProject)) {
        							_monitor.setTaskName("retrieving dependencies in "+IvyPlugin.getFullRetrievePatternHerited(_javaProject));
        							_ivy.retrieve(
        									md.getModuleRevisionId(), 
        									IvyPlugin.getFullRetrievePatternHerited(_javaProject),
        									new RetrieveOptions().setConfs(confs));
        						}
        					}
        				} catch (FileNotFoundException e) {
        					status[0] = new Status(Status.ERROR, IvyPlugin.ID, Status.ERROR, "ivy file not found: "+_ivyXmlFile+"\nPlease configure your IvyDE ClasspathContainer properly", e);
        					return;
        				} catch (ParseException e) {
        					status[0] = new Status(Status.ERROR, IvyPlugin.ID, Status.ERROR, "parse exception in: "+_ivyXmlFile+"\n"+e.getMessage(), e);
        					return;
        				} finally {
        					Thread.currentThread().setContextClassLoader(old);
        				}
        				ModuleId mid = md.getModuleRevisionId().getModuleId();

        				try {
        					if (!resolved) {
        						Message.info("\n\nIVY DE: using cached data of previous resolve of "+md.getModuleRevisionId().getModuleId()+"\n");
        					}
        					classpathItems[0] = parseResolvedConfs(confs, mid);
        				} catch (Exception ex) {
        					if (!resolved) {
        						//maybe this is a problem with the cache, we retry with an actual resolve
        						Message.info("\n\nIVYDE: tryed to build classpath from cache, but files seemed to be corrupted... trying with an actual resolve");
        						ResolveReport report = _ivy.resolve(ivyURL, new ResolveOptions().setConfs(_confs));
        						classpathItems[0] = parseResolvedConfs(report.getConfigurations(), mid);
        					}
        				}
        			} catch (Exception e) {
        				status[0] = new Status(Status.ERROR, IvyPlugin.ID, Status.ERROR, "An internal error occured while resolving dependencies of "+_ivyXmlFile+"\nPlease see eclipse error log and IvyConsole for details", e);
        				return;
        			} finally {
        				_monitor.done();
            			_ivy.getEventManager().removeIvyListener(IvyResolveJob.this);
        			}
        			
	    			if (!problemMessages.isEmpty()) {
	    				StringBuffer problems = new StringBuffer();
	    				for (Iterator iter = problemMessages.iterator(); iter.hasNext();) {
							String msg = (String) iter.next();
							problems.append(msg).append("\n");
						}
	    				status[0] = new Status(Status.ERROR, IvyPlugin.ID, Status.ERROR, "Impossible to resolve dependencies of "+md.getModuleRevisionId()+":\n"+problems+"\nSee IvyConsole for further details", null);
	    				return;
	    			} else {
	    				status[0] = Status.OK_STATUS;
	    				return;
	    			}
        		}
        	};
        	
        	try {
        		resolver.start();
        		while (true) {
        			try {
        				resolver.join(100);
        			} catch (InterruptedException e) {
        				_ivy.interrupt(resolver);
        				return Status.CANCEL_STATUS;
        			}
        			synchronized (status) { // ensure proper sharing of done var
        				if (status[0] != null || !resolver.isAlive()) {
        					break;
        				}
        			}
        			if (_monitor.isCanceled()) {
        				_ivy.interrupt(resolver);
        				return Status.CANCEL_STATUS;
        			}
        		}
        		if (status[0] == Status.OK_STATUS) {
	    			updateClasspathEntries(_usePreviousResolveIfExist, _notify, classpathItems[0]);
        		}
        		return status[0];
        	} finally {
        		synchronized (IvyClasspathContainer.this) {
        			_job = null;
				}
                IvyPlugin.log(IStatus.INFO, "resolved dependencies of "+_ivyXmlFile, null); 
        	}
        	

        }

		private ClasspathItem[] parseResolvedConfs(String[] confs, ModuleId mid) throws ParseException, IOException {
			ClasspathItem[] classpathItems;
            Collection all = new LinkedHashSet();
            String resolveId = ResolveOptions.getDefaultResolveId(mid);
            for (int i = 0; i < confs.length; i++) {
            	XmlReportParser parser = new XmlReportParser();
            	File report = _cacheMgr.getConfigurationResolveReportInCache(resolveId, confs[i]);
            	parser.parse(report);
                Artifact[] artifacts = parser.getArtifacts();
                all.addAll(Arrays.asList(artifacts));
            }
            Collection files = new LinkedHashSet();
            for (Iterator iter = all.iterator(); iter.hasNext();) {
                Artifact artifact = (Artifact)iter.next();
                if (IvyPlugin.accept(_javaProject, artifact)) {
                	File sourcesArtifact = getSourcesArtifact(artifact, all);
                	File javadocArtifact = getJavadocArtifact(artifact, all);
                	files.add(new ClasspathItem(
                			_cacheMgr.getArchiveFileInCache(artifact),
                			sourcesArtifact, 
                			javadocArtifact
                		));
                }
            }
            classpathItems = (ClasspathItem[])files.toArray(new ClasspathItem[files.size()]);
            
            return classpathItems;
        }

		private File getSourcesArtifact(Artifact artifact, Collection all)
		{
            for (Iterator iter = all.iterator(); iter.hasNext();) {
                Artifact a = (Artifact)iter.next();
                if (a.getName().equals(artifact.getName()) &&
                		a.getId().getRevision().equals(artifact.getId().getRevision()) &&
                		IvyPlugin.isSources(_javaProject, a))
                {
                	return _cacheMgr.getArchiveFileInCache(a);
                }
            }
            if (IvyPlugin.shouldTestNonDeclaredSources(_javaProject)) {
            	// source artifact not found in resolved artifacts, 
            	// try to see if a non declared one is available
            	Map extraAtt = new HashMap(artifact.getExtraAttributes());
            	extraAtt.put("classifier", "sources");
            	Artifact sourceArtifact = new DefaultArtifact(
            			artifact.getModuleRevisionId(),
            			artifact.getPublicationDate(),
            			artifact.getName(),
            			"source",
            			"jar",
            			extraAtt
            	);
            	_ivy.getResolveEngine().download(sourceArtifact, _cacheMgr, false);
            	File source = _cacheMgr.getArchiveFileInCache(sourceArtifact);
            	return source.exists()?source:null;
            } else {
            	return null;
            }
		}

		private File getJavadocArtifact(Artifact artifact, Collection all)
		{
			for (Iterator iter = all.iterator(); iter.hasNext();) {
				Artifact a = (Artifact)iter.next();
				if (a.getName().equals(artifact.getName()) &&
						a.getModuleRevisionId().equals(artifact.getModuleRevisionId()) &&
						a.getId().equals(artifact.getId()) &&
						IvyPlugin.isJavadoc(_javaProject, a))
				{
					return _cacheMgr.getArchiveFileInCache(a);
				}
			}
			if (IvyPlugin.shouldTestNonDeclaredSources(_javaProject)) {
				// javadoc artifact not found in resolved artifacts, 
				// try to see if a non declared one is available
				Map extraAtt = new HashMap(artifact.getExtraAttributes());
				extraAtt.put("classifier", "javadocs");
				Artifact javadocArtifact = new DefaultArtifact(
						artifact.getModuleRevisionId(),
						artifact.getPublicationDate(),
						artifact.getName(),
						"javadoc",
						"jar",
						extraAtt
				);
				_ivy.getResolveEngine().download(javadocArtifact, _cacheMgr, false);
				File javadoc = _cacheMgr.getArchiveFileInCache(javadocArtifact);
				return javadoc.exists()?javadoc:null;
			} else {
            	return null;
            }
		}
    }
    
    public class ClasspathItem
    {
    	private File classpathArtifact;
    	private File sourcesArtifact;
    	private File javadocArtifact;
    	
    	public ClasspathItem(File classpathArtifact, File sourcesArtifact, File javadocArtifact)
    	{
    		this.classpathArtifact = classpathArtifact;
    		this.sourcesArtifact = sourcesArtifact;
    		this.javadocArtifact = javadocArtifact;
    	}
    	
    	public Path getClasspathArtifactPath() {
    		return new Path(classpathArtifact.getAbsolutePath());
    	}
    	
    	public Path getSourcesArtifactPath() {
    		return (sourcesArtifact != null) ? new Path(sourcesArtifact.getAbsolutePath()) : null;
    	}

    	public Path getJavadocArtifactPath() {
    		return (javadocArtifact != null) ? new Path(javadocArtifact.getAbsolutePath()) : null;
    	}
    }

    public static final String IVY_CLASSPATH_CONTAINER_ID = "org.apache.ivyde.eclipse.cpcontainer.IVYDE_CONTAINER";
    
	private IClasspathEntry[] _classpathEntries;
	private IJavaProject _javaProject;
	
	
    private File _ivyXmlFile;
    private String _ivyXmlPath;
	private String[] _confs = new String[] { "default" };

    private IPath _path;
    
	private IvyResolveJob _job;


	public IvyClasspathContainer(IJavaProject javaProject, IPath path, String ivyFile, String[] confs ) {
		_javaProject = javaProject;
        _path = path;
		
        _ivyXmlPath = ivyFile;
		_ivyXmlFile = resolveFile( ivyFile );
        _confs = confs;   
        //do  execute this job in current thread 
        computeClasspathEntries(true, false).run(new NullProgressMonitor());
        if (_classpathEntries == null) {
            _classpathEntries = new IClasspathEntry[0];
        }
        IvyPlugin.getDefault().register(this);
	}
	
    private File resolveFile( String path ) {
		IFile iFile = _javaProject.getProject().getFile( path );
		return new File( iFile.getLocation().toOSString() );
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
	 */
	public String getDescription() {
		return _ivyXmlPath+" "+Arrays.asList(_confs);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
	 */
	public int getKind() {
		return K_APPLICATION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
	 */
	public IPath getPath() {
		return _path;
	}
	
	
    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
     */
    public IClasspathEntry[] getClasspathEntries() {
        return _classpathEntries;
    }

	private final static ISchedulingRule RESOLVE_EVENT_RULE = new ISchedulingRule() {
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return rule == this;
		}
	};
    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
     */
    private IvyResolveJob computeClasspathEntries(final boolean usePreviousResolveIfExist, boolean notify) {
        try {
        	Ivy ivy = IvyPlugin.getIvy(_javaProject);

        	// resolve job already running
        	synchronized (this) {
	        	if (_job != null) {
	        		return _job;
	        	}
	        	_job = new IvyResolveJob(ivy, usePreviousResolveIfExist, notify);
	        	_job.setUser(true);
	        	_job.setRule(RESOLVE_EVENT_RULE);
	        	return _job;
        	}
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }        
    }

    /**
     * This method is here to available the Resolve all action to run in a single progress window.
     * It is quiet ugly but it is a first way to do this quiet quickly.
     * @param monitor
     */
    public void resolve(IProgressMonitor monitor) {
        computeClasspathEntries(false, true).run(monitor);
    }
    
    public void resolve() {
        computeClasspathEntries(false, true).schedule();
    }
    public void refresh() {
        computeClasspathEntries(true, true).schedule();
    }


    private void updateClasspathEntries(final boolean usePreviousResolveIfExist, boolean notify, final ClasspathItem[] classpathItems) {
    	if(classpathItems != null) {
	        IClasspathEntry[] entries = new IClasspathEntry[ classpathItems.length ];
	        
	        for( int i=0; i<classpathItems.length; i++ ) {
	            Path path = classpathItems[ i ].getClasspathArtifactPath();
	            entries[ i ] = JavaCore.newLibraryEntry(path, 
	                                                    getSourceAttachment(classpathItems[ i ]), 
	                                                    getSourceAttachmentRoot(classpathItems[ i ]),
	                                                    ClasspathEntry.NO_ACCESS_RULES,
	                                                    getExtraAttribute(classpathItems[ i ]),
	                                                    false);
	        }
	        setClasspathEntries(entries);
    	} else {
            setClasspathEntries(new IClasspathEntry[0]);
    	}
    	if (notify) {
    		notifyUpdateClasspathEntries();
    	}
    }

    private IPath getSourceAttachment(ClasspathItem classpathItem) {
    	IPath sourceAttachment = IvyPlugin.getDefault().getPackageFragmentExtraInfo().getSourceAttachment(classpathItem.getClasspathArtifactPath());
    	if (sourceAttachment == null)
    		sourceAttachment = classpathItem.getSourcesArtifactPath();
    		
    	return sourceAttachment;
    }
    
    private IPath getSourceAttachmentRoot(ClasspathItem classpathItem) {
    	IPath sourceAttachment = IvyPlugin.getDefault().getPackageFragmentExtraInfo().getSourceAttachmentRoot(classpathItem.getClasspathArtifactPath());
    	if (sourceAttachment == null)
    		sourceAttachment = classpathItem.getSourcesArtifactPath();
    		
    	return sourceAttachment;
    }

    private IClasspathAttribute[] getExtraAttribute(ClasspathItem classpathItem) {
        List result  = new ArrayList();
        IPath p = IvyPlugin.getDefault().getPackageFragmentExtraInfo().getDocAttachment(classpathItem.getClasspathArtifactPath());
        
        if (p == null)
        	p = classpathItem.getJavadocArtifactPath();
        	
        if(p != null) {
            result.add(new ClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, p.toPortableString()));
        }
        return (IClasspathAttribute[]) result.toArray(new IClasspathAttribute[result.size()]);
    }



    private void setClasspathEntries(final IClasspathEntry[] entries) {
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                _classpathEntries = entries;
            }
        });
    }

    private void notifyUpdateClasspathEntries() {
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                try {
                    JavaModelManager manager = JavaModelManager.getJavaModelManager();
                    manager.containerPut(_javaProject, _path, null);
                    JavaCore.setClasspathContainer(
                            _path,
                            new IJavaProject[] {_javaProject},
                            new IClasspathContainer[] {IvyClasspathContainer.this},
                            null);
                } catch (JavaModelException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }                    
            }
        });
    }

    public static String[] getConfigurations(IPath containerPath) {
        return containerPath.lastSegment().split(",");
    }

    public static String getConfigurationsText(IPath containerPath) {
        return containerPath.lastSegment();
    }


    public static String getIvyFilePath(IPath containerPath) {
        return ((IPath)containerPath.clone()).removeFirstSegments(1).removeLastSegments(1).toString();
    }
    
    
    public static boolean isIvyClasspathContainer(IPath containerPath) {
        return containerPath.segmentCount() >= 3 && IvyClasspathContainer.IVY_CLASSPATH_CONTAINER_ID.equals(containerPath.segment(0)) ;
    }

    /**
     * Resolves the classpath container corresponding to the given ivy file, if any.
     * @param file
     */
    public static void resolveIfNeeded(IFile file) {
        IJavaProject javaProject = JavaCore.create(file.getProject());
        try {
            IClasspathEntry[] entries = javaProject.getRawClasspath();
            for (int i= 0; i < entries.length; i++) {
                IClasspathEntry entry= entries[i];
                if (entry != null && entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) { 
                    IPath path = entry.getPath();
                    if (isIvyClasspathContainer(path) && file.getProjectRelativePath().toString().equals(getIvyFilePath(path))) {
                        IClasspathContainer cp = JavaCore.getClasspathContainer(path, javaProject);
                        
                        if (cp instanceof IvyClasspathContainer) {
                            IvyClasspathContainer c = (IvyClasspathContainer)cp;
                            c.resolve();
                        }
                    }
                }
            }
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
    }

    public static void resolve(IJavaProject javaProject) {
        try {
            IClasspathEntry[] entries = javaProject.getRawClasspath();
            for (int i= 0; i < entries.length; i++) {
                IClasspathEntry entry= entries[i];
                if (entry != null && entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) { 
                    IPath path = entry.getPath();
                    if (isIvyClasspathContainer(path)) {
                        IClasspathContainer cp = JavaCore.getClasspathContainer(path, javaProject);
                        
                        if (cp instanceof IvyClasspathContainer) {
                            IvyClasspathContainer c = (IvyClasspathContainer)cp;
                            c.resolve();
                        }
                    }
                }
            }
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
    }

    public IFile getIvyFile() {
        return _javaProject.getProject().getFile(_ivyXmlPath);
    }

    public URL getReportUrl() {
    	try {
    		Ivy ivy = IvyPlugin.getIvy(_javaProject);
    		URL ivyURL = _ivyXmlFile.toURL();
    		ModuleDescriptor md = ModuleDescriptorParserRegistry.getInstance().parseDescriptor(ivy.getSettings(), ivyURL, false);
    		String resolveId = ResolveOptions.getDefaultResolveId(md);
    		return CacheManager.getInstance(ivy.getSettings())
    			.getConfigurationResolveReportInCache(
    					resolveId, 
    					md.getConfigurationsNames()[0]).toURL();
    	} catch (Exception ex) {
    		return null;
    	}
    }

	public IJavaProject getProject() {
		return _javaProject;
	}
}