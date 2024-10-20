package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.artifacts.dsl.CapabilityNotationParser;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the `libs` extension.
 */
@NonNullApi
public class LibrariesForLibs extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final AndroidxLibraryAccessors laccForAndroidxLibraryAccessors = new AndroidxLibraryAccessors(owner);
    private final KotlinxLibraryAccessors laccForKotlinxLibraryAccessors = new KotlinxLibraryAccessors(owner);
    private final MaterialLibraryAccessors laccForMaterialLibraryAccessors = new MaterialLibraryAccessors(owner);
    private final SevenzipjbindingLibraryAccessors laccForSevenzipjbindingLibraryAccessors = new SevenzipjbindingLibraryAccessors(owner);
    private final TruevfsLibraryAccessors laccForTruevfsLibraryAccessors = new TruevfsLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibs(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

        /**
         * Creates a dependency provider for glide (com.github.bumptech.glide:glide)
     * with versionRef 'glide'.
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getGlide() {
            return create("glide");
    }

        /**
         * Creates a dependency provider for junit (junit:junit)
     * with versionRef 'junit'.
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJunit() {
            return create("junit");
    }

        /**
         * Creates a dependency provider for zip4j (net.lingala.zip4j:zip4j)
     * with versionRef 'zip4j'.
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getZip4j() {
            return create("zip4j");
    }

    /**
     * Returns the group of libraries at androidx
     */
    public AndroidxLibraryAccessors getAndroidx() {
        return laccForAndroidxLibraryAccessors;
    }

    /**
     * Returns the group of libraries at kotlinx
     */
    public KotlinxLibraryAccessors getKotlinx() {
        return laccForKotlinxLibraryAccessors;
    }

    /**
     * Returns the group of libraries at material
     */
    public MaterialLibraryAccessors getMaterial() {
        return laccForMaterialLibraryAccessors;
    }

    /**
     * Returns the group of libraries at sevenzipjbinding
     */
    public SevenzipjbindingLibraryAccessors getSevenzipjbinding() {
        return laccForSevenzipjbindingLibraryAccessors;
    }

    /**
     * Returns the group of libraries at truevfs
     */
    public TruevfsLibraryAccessors getTruevfs() {
        return laccForTruevfsLibraryAccessors;
    }

    /**
     * Returns the group of versions at versions
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Returns the group of bundles at bundles
     */
    public BundleAccessors getBundles() {
        return baccForBundleAccessors;
    }

    /**
     * Returns the group of plugins at plugins
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    public static class AndroidxLibraryAccessors extends SubDependencyFactory {
        private final AndroidxCoreLibraryAccessors laccForAndroidxCoreLibraryAccessors = new AndroidxCoreLibraryAccessors(owner);
        private final AndroidxEspressoLibraryAccessors laccForAndroidxEspressoLibraryAccessors = new AndroidxEspressoLibraryAccessors(owner);
        private final AndroidxFragmentLibraryAccessors laccForAndroidxFragmentLibraryAccessors = new AndroidxFragmentLibraryAccessors(owner);
        private final AndroidxRoomLibraryAccessors laccForAndroidxRoomLibraryAccessors = new AndroidxRoomLibraryAccessors(owner);

        public AndroidxLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for activity (androidx.activity:activity)
         * with versionRef 'activity'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getActivity() {
                return create("androidx.activity");
        }

            /**
             * Creates a dependency provider for appcompat (androidx.appcompat:appcompat)
         * with versionRef 'appcompat'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getAppcompat() {
                return create("androidx.appcompat");
        }

            /**
             * Creates a dependency provider for constraintlayout (androidx.constraintlayout:constraintlayout)
         * with versionRef 'constraintlayout'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getConstraintlayout() {
                return create("androidx.constraintlayout");
        }

            /**
             * Creates a dependency provider for junit (androidx.test.ext:junit)
         * with versionRef 'junitVersion'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getJunit() {
                return create("androidx.junit");
        }

        /**
         * Returns the group of libraries at androidx.core
         */
        public AndroidxCoreLibraryAccessors getCore() {
            return laccForAndroidxCoreLibraryAccessors;
        }

        /**
         * Returns the group of libraries at androidx.espresso
         */
        public AndroidxEspressoLibraryAccessors getEspresso() {
            return laccForAndroidxEspressoLibraryAccessors;
        }

        /**
         * Returns the group of libraries at androidx.fragment
         */
        public AndroidxFragmentLibraryAccessors getFragment() {
            return laccForAndroidxFragmentLibraryAccessors;
        }

        /**
         * Returns the group of libraries at androidx.room
         */
        public AndroidxRoomLibraryAccessors getRoom() {
            return laccForAndroidxRoomLibraryAccessors;
        }

    }

    public static class AndroidxCoreLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public AndroidxCoreLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for core (androidx.core:core)
         * with versionRef 'coreKtx'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> asProvider() {
                return create("androidx.core");
        }

            /**
             * Creates a dependency provider for ktx (androidx.core:core-ktx)
         * with versionRef 'coreKtx'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getKtx() {
                return create("androidx.core.ktx");
        }

    }

    public static class AndroidxEspressoLibraryAccessors extends SubDependencyFactory {

        public AndroidxEspressoLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for core (androidx.test.espresso:espresso-core)
         * with versionRef 'espressoCore'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getCore() {
                return create("androidx.espresso.core");
        }

    }

    public static class AndroidxFragmentLibraryAccessors extends SubDependencyFactory {

        public AndroidxFragmentLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for ktx (androidx.fragment:fragment-ktx)
         * with versionRef 'fragmentKtx'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getKtx() {
                return create("androidx.fragment.ktx");
        }

    }

    public static class AndroidxRoomLibraryAccessors extends SubDependencyFactory {
        private final AndroidxRoomCompilerLibraryAccessors laccForAndroidxRoomCompilerLibraryAccessors = new AndroidxRoomCompilerLibraryAccessors(owner);
        private final AndroidxRoomKtxLibraryAccessors laccForAndroidxRoomKtxLibraryAccessors = new AndroidxRoomKtxLibraryAccessors(owner);
        private final AndroidxRoomRuntimeLibraryAccessors laccForAndroidxRoomRuntimeLibraryAccessors = new AndroidxRoomRuntimeLibraryAccessors(owner);

        public AndroidxRoomLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Returns the group of libraries at androidx.room.compiler
         */
        public AndroidxRoomCompilerLibraryAccessors getCompiler() {
            return laccForAndroidxRoomCompilerLibraryAccessors;
        }

        /**
         * Returns the group of libraries at androidx.room.ktx
         */
        public AndroidxRoomKtxLibraryAccessors getKtx() {
            return laccForAndroidxRoomKtxLibraryAccessors;
        }

        /**
         * Returns the group of libraries at androidx.room.runtime
         */
        public AndroidxRoomRuntimeLibraryAccessors getRuntime() {
            return laccForAndroidxRoomRuntimeLibraryAccessors;
        }

    }

    public static class AndroidxRoomCompilerLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public AndroidxRoomCompilerLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for compiler (androidx.room:room-compiler)
         * with versionRef 'roomRuntime'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> asProvider() {
                return create("androidx.room.compiler");
        }

            /**
             * Creates a dependency provider for v250 (androidx.room:room-compiler)
         * with versionRef 'roomCompiler'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getV250() {
                return create("androidx.room.compiler.v250");
        }

            /**
             * Creates a dependency provider for v261 (androidx.room:room-compiler)
         * with versionRef 'roomCompilerVersion'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getV261() {
                return create("androidx.room.compiler.v261");
        }

    }

    public static class AndroidxRoomKtxLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public AndroidxRoomKtxLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for ktx (androidx.room:room-ktx)
         * with versionRef 'roomRuntime'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> asProvider() {
                return create("androidx.room.ktx");
        }

            /**
             * Creates a dependency provider for v261 (androidx.room:room-ktx)
         * with versionRef 'roomKtx'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getV261() {
                return create("androidx.room.ktx.v261");
        }

    }

    public static class AndroidxRoomRuntimeLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public AndroidxRoomRuntimeLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for runtime (androidx.room:room-runtime)
         * with versionRef 'roomRuntime'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> asProvider() {
                return create("androidx.room.runtime");
        }

            /**
             * Creates a dependency provider for v261 (androidx.room:room-runtime)
         * with versionRef 'roomRuntimeVersion'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getV261() {
                return create("androidx.room.runtime.v261");
        }

    }

    public static class KotlinxLibraryAccessors extends SubDependencyFactory {
        private final KotlinxCoroutinesLibraryAccessors laccForKotlinxCoroutinesLibraryAccessors = new KotlinxCoroutinesLibraryAccessors(owner);

        public KotlinxLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Returns the group of libraries at kotlinx.coroutines
         */
        public KotlinxCoroutinesLibraryAccessors getCoroutines() {
            return laccForKotlinxCoroutinesLibraryAccessors;
        }

    }

    public static class KotlinxCoroutinesLibraryAccessors extends SubDependencyFactory {
        private final KotlinxCoroutinesAndroidLibraryAccessors laccForKotlinxCoroutinesAndroidLibraryAccessors = new KotlinxCoroutinesAndroidLibraryAccessors(owner);
        private final KotlinxCoroutinesCoreLibraryAccessors laccForKotlinxCoroutinesCoreLibraryAccessors = new KotlinxCoroutinesCoreLibraryAccessors(owner);

        public KotlinxCoroutinesLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Returns the group of libraries at kotlinx.coroutines.android
         */
        public KotlinxCoroutinesAndroidLibraryAccessors getAndroid() {
            return laccForKotlinxCoroutinesAndroidLibraryAccessors;
        }

        /**
         * Returns the group of libraries at kotlinx.coroutines.core
         */
        public KotlinxCoroutinesCoreLibraryAccessors getCore() {
            return laccForKotlinxCoroutinesCoreLibraryAccessors;
        }

    }

    public static class KotlinxCoroutinesAndroidLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public KotlinxCoroutinesAndroidLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for android (org.jetbrains.kotlinx:kotlinx-coroutines-android)
         * with versionRef 'kotlinxCoroutinesCore'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> asProvider() {
                return create("kotlinx.coroutines.android");
        }

            /**
             * Creates a dependency provider for v163 (org.jetbrains.kotlinx:kotlinx-coroutines-android)
         * with versionRef 'kotlinxCoroutinesAndroid'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getV163() {
                return create("kotlinx.coroutines.android.v163");
        }

    }

    public static class KotlinxCoroutinesCoreLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public KotlinxCoroutinesCoreLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for core (org.jetbrains.kotlinx:kotlinx-coroutines-core)
         * with versionRef 'kotlinxCoroutinesCore'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> asProvider() {
                return create("kotlinx.coroutines.core");
        }

            /**
             * Creates a dependency provider for v163 (org.jetbrains.kotlinx:kotlinx-coroutines-core)
         * with versionRef 'kotlinxCoroutinesCoreVersion'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getV163() {
                return create("kotlinx.coroutines.core.v163");
        }

    }

    public static class MaterialLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public MaterialLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for material (com.google.android.material:material)
         * with versionRef 'material'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> asProvider() {
                return create("material");
        }

            /**
             * Creates a dependency provider for v11210 (com.google.android.material:material)
         * with versionRef 'materialVersion'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getV11210() {
                return create("material.v11210");
        }

    }

    public static class SevenzipjbindingLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {
        private final SevenzipjbindingAllLibraryAccessors laccForSevenzipjbindingAllLibraryAccessors = new SevenzipjbindingAllLibraryAccessors(owner);

        public SevenzipjbindingLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for sevenzipjbinding (net.sf.sevenzipjbinding:sevenzipjbinding)
         * with versionRef 'sevenzipjbindingAllPlatforms'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> asProvider() {
                return create("sevenzipjbinding");
        }

        /**
         * Returns the group of libraries at sevenzipjbinding.all
         */
        public SevenzipjbindingAllLibraryAccessors getAll() {
            return laccForSevenzipjbindingAllLibraryAccessors;
        }

    }

    public static class SevenzipjbindingAllLibraryAccessors extends SubDependencyFactory {

        public SevenzipjbindingAllLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for platforms (net.sf.sevenzipjbinding:sevenzipjbinding-all-platforms)
         * with versionRef 'sevenzipjbindingAllPlatforms'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getPlatforms() {
                return create("sevenzipjbinding.all.platforms");
        }

    }

    public static class TruevfsLibraryAccessors extends SubDependencyFactory {
        private final TruevfsKernelLibraryAccessors laccForTruevfsKernelLibraryAccessors = new TruevfsKernelLibraryAccessors(owner);

        public TruevfsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Returns the group of libraries at truevfs.kernel
         */
        public TruevfsKernelLibraryAccessors getKernel() {
            return laccForTruevfsKernelLibraryAccessors;
        }

    }

    public static class TruevfsKernelLibraryAccessors extends SubDependencyFactory {

        public TruevfsKernelLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for impl (net.java.truevfs:truevfs-kernel-impl)
         * with versionRef 'truevfsKernelImpl'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getImpl() {
                return create("truevfs.kernel.impl");
        }

    }

    public static class VersionAccessors extends VersionFactory  {

        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

            /**
             * Returns the version associated to this alias: activity (1.9.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getActivity() { return getVersion("activity"); }

            /**
             * Returns the version associated to this alias: agp (8.7.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getAgp() { return getVersion("agp"); }

            /**
             * Returns the version associated to this alias: appcompat (1.7.0)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getAppcompat() { return getVersion("appcompat"); }

            /**
             * Returns the version associated to this alias: constraintlayout (2.1.4)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getConstraintlayout() { return getVersion("constraintlayout"); }

            /**
             * Returns the version associated to this alias: coreKtx (1.13.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getCoreKtx() { return getVersion("coreKtx"); }

            /**
             * Returns the version associated to this alias: espressoCore (3.6.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getEspressoCore() { return getVersion("espressoCore"); }

            /**
             * Returns the version associated to this alias: fragmentKtx (1.6.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getFragmentKtx() { return getVersion("fragmentKtx"); }

            /**
             * Returns the version associated to this alias: glide (4.16.0)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getGlide() { return getVersion("glide"); }

            /**
             * Returns the version associated to this alias: junit (4.13.2)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getJunit() { return getVersion("junit"); }

            /**
             * Returns the version associated to this alias: junitVersion (1.2.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getJunitVersion() { return getVersion("junitVersion"); }

            /**
             * Returns the version associated to this alias: kotlin (1.9.0)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getKotlin() { return getVersion("kotlin"); }

            /**
             * Returns the version associated to this alias: kotlinxCoroutinesAndroid (1.6.3)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getKotlinxCoroutinesAndroid() { return getVersion("kotlinxCoroutinesAndroid"); }

            /**
             * Returns the version associated to this alias: kotlinxCoroutinesCore (1.7.3)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getKotlinxCoroutinesCore() { return getVersion("kotlinxCoroutinesCore"); }

            /**
             * Returns the version associated to this alias: kotlinxCoroutinesCoreVersion (1.6.3)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getKotlinxCoroutinesCoreVersion() { return getVersion("kotlinxCoroutinesCoreVersion"); }

            /**
             * Returns the version associated to this alias: material (1.12.0)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getMaterial() { return getVersion("material"); }

            /**
             * Returns the version associated to this alias: materialVersion (1.12.10)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getMaterialVersion() { return getVersion("materialVersion"); }

            /**
             * Returns the version associated to this alias: roomCompiler (2.5.0)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getRoomCompiler() { return getVersion("roomCompiler"); }

            /**
             * Returns the version associated to this alias: roomCompilerVersion (2.6.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getRoomCompilerVersion() { return getVersion("roomCompilerVersion"); }

            /**
             * Returns the version associated to this alias: roomKtx (2.6.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getRoomKtx() { return getVersion("roomKtx"); }

            /**
             * Returns the version associated to this alias: roomRuntime (2.5.2)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getRoomRuntime() { return getVersion("roomRuntime"); }

            /**
             * Returns the version associated to this alias: roomRuntimeVersion (2.6.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getRoomRuntimeVersion() { return getVersion("roomRuntimeVersion"); }

            /**
             * Returns the version associated to this alias: sevenzipjbindingAllPlatforms (16.02-2.01)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getSevenzipjbindingAllPlatforms() { return getVersion("sevenzipjbindingAllPlatforms"); }

            /**
             * Returns the version associated to this alias: truevfsKernelImpl (0.10.5)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getTruevfsKernelImpl() { return getVersion("truevfsKernelImpl"); }

            /**
             * Returns the version associated to this alias: zip4j (2.11.5)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getZip4j() { return getVersion("zip4j"); }

    }

    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

    }

    public static class PluginAccessors extends PluginFactory {
        private final AndroidPluginAccessors paccForAndroidPluginAccessors = new AndroidPluginAccessors(providers, config);
        private final KotlinPluginAccessors paccForKotlinPluginAccessors = new KotlinPluginAccessors(providers, config);

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Returns the group of plugins at plugins.android
         */
        public AndroidPluginAccessors getAndroid() {
            return paccForAndroidPluginAccessors;
        }

        /**
         * Returns the group of plugins at plugins.kotlin
         */
        public KotlinPluginAccessors getKotlin() {
            return paccForKotlinPluginAccessors;
        }

    }

    public static class AndroidPluginAccessors extends PluginFactory {

        public AndroidPluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

            /**
             * Creates a plugin provider for android.application to the plugin id 'com.android.application'
             * with versionRef 'agp'.
             * This plugin was declared in catalog libs.versions.toml
             */
            public Provider<PluginDependency> getApplication() { return createPlugin("android.application"); }

    }

    public static class KotlinPluginAccessors extends PluginFactory {

        public KotlinPluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

            /**
             * Creates a plugin provider for kotlin.android to the plugin id 'org.jetbrains.kotlin.android'
             * with versionRef 'kotlin'.
             * This plugin was declared in catalog libs.versions.toml
             */
            public Provider<PluginDependency> getAndroid() { return createPlugin("kotlin.android"); }

    }

}
