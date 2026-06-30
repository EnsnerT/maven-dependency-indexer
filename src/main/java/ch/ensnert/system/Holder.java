package ch.ensnert.system;

import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectModelResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Plexus Managed Instances holder
 *
 * @author ensnerT (2026) - no AI was used
 */
@Named()
@Singleton()
public final class Holder
{
	private RepositorySystem repositorySystem;
	private RemoteRepositoryManager repositoryManager;
	private DefaultModelBuilder modelBuilder;
	private ArrayList<RemoteRepository> remoteRepository;
	private DefaultRepositorySystemSession repositorySystemSession;
	private ProjectModelResolver modelResolver;

	@Inject()
	public Holder(RepositorySystem repositorySystem, RemoteRepositoryManager repoMgr)
	{
		if (repositorySystem != null)
			this.repositorySystem = repositorySystem;

		if (repoMgr != null)
			this.repositoryManager = repoMgr;
	}

	public RepositorySystem getRepositorySystem()
	{
		return this.repositorySystem;
	}

	public RemoteRepositoryManager getRepositoryManager()
	{
		return this.repositoryManager;
	}

	public ModelBuilder getModelBuilder()
	{
		if (modelBuilder == null)
		{
			modelBuilder = new DefaultModelBuilderFactory().newInstance();
		}

		return modelBuilder;
	}

	public List<RemoteRepository> getRemoteRepositories()
	{
		if (remoteRepository == null)
		{
			remoteRepository = new ArrayList<>();
			remoteRepository.add(
					new org.eclipse.aether.repository.RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build());
		}
		return remoteRepository;
	}

	public RepositorySystemSession getRepositorySystemSession()
	{
		if (repositorySystemSession == null)
		{
			repositorySystemSession = MavenRepositorySystemUtils.newSession();
			repositorySystemSession.setLocalRepositoryManager(getRepositorySystem().newLocalRepositoryManager(repositorySystemSession,
					new LocalRepository(new File(System.getProperty("user.home"), ".m2/repository"))));
		}
		return repositorySystemSession;
	}

	public ModelResolver getModelResolver()
	{
		if (modelResolver == null)
		{
			modelResolver = new ProjectModelResolver(getRepositorySystemSession(), new RequestTrace(null), getRepositorySystem(), getRepositoryManager(),
					getRemoteRepositories(), ProjectBuildingRequest.RepositoryMerging.POM_DOMINANT, null);
		}
		return modelResolver;
	}

}
