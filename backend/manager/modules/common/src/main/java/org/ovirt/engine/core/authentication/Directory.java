package org.ovirt.engine.core.authentication;

import java.util.List;

import org.ovirt.engine.core.common.utils.ExternalId;

/**
 * A directory is an object that manages a collection of users and groups, usually stored in an external system like an
 * LDAP database.
 */
public interface Directory {
    /**
     * Returns the name of the directory.
     *
     * @return the name of the directory
     */
    String getName();

    /**
     * Retrieves a user from the directory given its name. The name is expected to be unique.
     *
     * @param name the name of the user
     * @return the user corresponding to the given name or {@code null} if no such user can be found
     */
    DirectoryUser findUser(String name);

    /**
     * Retrieves a user from the directory given its identifier.
     *
     * @param id the identifier of the user
     * @return the user corresponding to the given identifier or {@code null} if no such user can be found
     */
    DirectoryUser findUser(ExternalId id);

    /**
     * Retrieves a list of users from the directory given their identifiers.
     *
     * @param ids the list of identifiers
     * @return a list containing at most on user for each identifier in the given set with no particular order, note
     *     that the returned list may contain less elements than the given list of identifiers
     */
    List<DirectoryUser> findUsers(List<ExternalId> ids);

    /**
     * Retrieves a group from the directory given its name.
     *
     * @param name the name of the group
     * @return the group corresponding to the given name or {@code null} if no such group can be found
     */
    DirectoryGroup findGroup(String name);

    /**
     * Retrieves a group from the directory given its identifier.
     *
     * @param id the identifier of the group
     * @return the group corresponding to the given identifier or {@code null} if no such group can be found
     */
    DirectoryGroup findGroup(ExternalId id);

    /**
     * Search the directory looking for users that match the given search query. Note that the query uses the LDAP query
     * format, regardless of the type of the directory, so the implementation is responsible for translating this LDAP
     * specific query into whatever is required by the underlying directory.
     *
     * @param query the LDAP query
     * @return a list containing the users that match the given query
     */
    List<DirectoryUser> queryUsers(String query);

    /**
     * Search the directory looking for groups that match the given search query. Note that the query uses the LDAP
     * query format, regardless of the type of the directory, so the implementation is responsible for translating this
     * LDAP specific query into whatever is required by the underlying directory.
     *
     * @param query the LDAP query
     * @return a list containing the groups that match the given query
     */
    List<DirectoryGroup> queryGroups(String query);
}