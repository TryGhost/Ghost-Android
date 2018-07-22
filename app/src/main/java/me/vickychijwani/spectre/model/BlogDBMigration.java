package me.vickychijwani.spectre.model;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmSchema;
import me.vickychijwani.spectre.util.log.Log;

public class BlogDBMigration implements RealmMigration {

    private static final String TAG = BlogMetadataDBMigration.class.getSimpleName();

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();
        Log.i(TAG, "MIGRATING BLOG DATA DB from v%d to v%d", oldVersion, newVersion);

        // why < 2? because I forgot to assign a schema version until I wrote this migration, so the
        // oldVersion here will be whatever default is assigned by Realm
        if (oldVersion < 2) {
            if (!schema.get("Post").hasField("customExcerpt")) {
                Log.d(TAG, "ADDING CUSTOM EXCERPT FIELD TO POST TABLE");
                schema.get("Post").addField("customExcerpt", String.class);
            }
            oldVersion = 2;
        }

        if (oldVersion == 2) {
            if (schema.get("Post").hasField("markdown")) {
                Log.i(TAG, "REMOVING MARKDOWN FIELD FROM POST TABLE");
                schema.get("Post").removeField("markdown");
            }
            ++oldVersion;
        }
    }

    // override equals and hashCode to prevent this error in e2e tests:
    //   IllegalArgumentException: Configurations cannot be different if used to open the same file.
    //   The most likely cause is that equals() and hashCode() are not overridden in the migration class
    // see https://stackoverflow.com/a/36919305/504611 for more
    @Override
    public int hashCode() {
        return 34578;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof BlogDBMigration);
    }

}
