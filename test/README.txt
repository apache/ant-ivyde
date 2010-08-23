This folder contains java projects to import into Eclipse, to functionally test
that IvyDE works correctly with different kinds of configuration.

Environment:
 * the global configuration of Ivy in the workspace is expected to be the
   default one (using the maven repository).

Expectation:
 * each IvyDE classpath container to resolve correctly
 * the projects are expected to compile correctly

Exception:
 * project 'linked-folder': it is relying on some Eclipse linked folder which
   requires some absolute path. You probably will require to change it to make
   it resolve correctly
 * project 'include-settings' will not resolve correctly if it is tried in an 
   Eclipse started from Eclipse in debug, and having the Ivy project opened the
   Eclipse host.

Warning: if you start an Eclipse with a recent IvyDE, most of these test
   projects will have their .project and .classpath modified. This is due to
   the migration happening on old project configurations. Please DO NOT commit
   these changes. So we will still be able to continue to test the migration of
   old projects.
