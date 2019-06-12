package io.getty.rntron.security.module;

import io.getty.rntron.security.model.UserSecret;
import io.realm.annotations.RealmModule;

@RealmModule(library = true, classes = { UserSecret.class })
public class Secrets {
}

