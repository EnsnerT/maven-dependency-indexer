package s;

import ch.ensnert.App;
import ch.ensnert.impl.Output;
import com.google.inject.AbstractModule;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.IOException;


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

	public static void main(String[] args) throws ComponentLookupException, PlexusContainerException, IOException
	{
		setupLogging();
		var ref = new Object()
		{
			ComponentLookupException error = null;
		};
		setupPlexus(() ->
		{
			try
			{
				getApp().run(args);
			}
			catch (ComponentLookupException e)
			{
				ref.error = e;
			}
		});
		if (ref.error != null)
		{
			throw ref.error;
		}
	}

	private static void setupLogging()
	{
		Output.setOut(System.out);
		Output.setErr(System.err);
	}

	public static App getApp() throws ComponentLookupException
	{
		return getContainer().lookup(App.class);
	}

	public static void setupPlexus(Runnable then) throws IOException, PlexusContainerException
	{
		if (classWorld == null)
		{
			classWorld = new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());
		}

		if (container == null)
		{
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

				container = new DefaultPlexusContainer(cc, new AbstractModule()
				{
					@Override
					protected void configure()
					{
						this.bind(App.class).to(AnalyzerApp.class).asEagerSingleton();
					}
				});

				container.setLookupRealm((ClassRealm) null);

				Thread.currentThread().setContextClassLoader(container.getContainerRealm()); // replace current class loader with the new instance

				then.run();
			}
		}

	}
}
