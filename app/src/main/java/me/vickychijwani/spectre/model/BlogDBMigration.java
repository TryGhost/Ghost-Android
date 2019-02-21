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

        if (oldVersion == 3) {
            // Issue #38: crash caused by null values received in API responses
            // Ghost v2.13 included a migration (that was later reverted) that replaced "" in
            // nullable fields with `null`. So we should base our nullability decisions on the
            // actual Ghost schema rather than making assumptions about what can or cannot be null.
            // See https://github.com/TryGhost/Ghost/blob/master/core/server/data/schema/schema.js
            schema.get("Role")
                    .setNullable("description", true);
            schema.get("Setting")
                    .setNullable("value", true);
            schema.get("ConfigurationParam")
                    .setNullable("value", true);

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
