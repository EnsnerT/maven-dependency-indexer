package s;

import ch.ensnert.App;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;


/**
 * This Loader Acts like Maven and creates an Runnable Bean Environment.
 */
public final class PlexusLoader
{
	private static ClassWorld classWorld;

	private static DefaultPlexusContainer container;

	public static DefaultPlexusContainer getContainer()
	{
		return container;
	}

	public static void main(String[] args)
	{
		if (classWorld == null)
		{
			classWorld = new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());
		}
		/// copied mainly from org.apache.maven.cli.MavenCli.container()
		try (ClassWorld world = classWorld)
		{
			ClassRealm realm = world.getClassRealm("plexus.core");
			if (realm == null)
			{
				realm = classWorld.getRealms().iterator().next();
			}
			// CoreExtensionEntry coreEntry = CoreExtensionEntry.discoverFrom(realm);

			ContainerConfiguration cc = (new DefaultContainerConfiguration()).setClassWorld(world).setRealm(realm).setClassPathScanning("on")
												// .setClassPathScanning("index")
												.setAutoWiring(true).setJSR250Lifecycle(true).setName("maven");
			// setJSR250Lifecycle might not be needed

			container = new DefaultPlexusContainer(cc, new Module[] { new AbstractModule()
			{
				@Override
				protected void configure()
				{
					this.bind(App.class).to(AnalyzerApp.class).asEagerSingleton();
				}
			} });

			container.setLookupRealm((ClassRealm) null);

			Thread.currentThread().setContextClassLoader(container.getContainerRealm()); // replace current class loader with the new instance

			// System.out.println("Plexus started");

			App main = container.lookup(App.class);
			main.run(args);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			// classWorld.close();
		}

	}
}
