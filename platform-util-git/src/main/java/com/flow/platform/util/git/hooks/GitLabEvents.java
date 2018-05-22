/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.util.git.hooks;

import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.model.GitEvent;
import com.flow.platform.util.git.model.GitEventType;
import com.flow.platform.util.git.model.GitPullRequestEvent;
import com.flow.platform.util.git.model.GitPullRequestEvent.State;
import com.flow.platform.util.git.model.GitPullRequestInfo;
import com.flow.platform.util.git.model.GitPushTagEvent;
import com.flow.platform.util.git.model.GitSource;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/**
 * To adding GitLab web hook,
 * should select 'Push events', 'Tag push events' and 'Merge Request events'
 *
 * @author yang
 */
public class GitLabEvents {

    public static class Hooks {

        public final static String HEADER = "x-gitlab-event";

        public final static String EVENT_TYPE_PUSH = "Push Hook";

        public final static String EVENT_TYPE_TAG = "Tag Push Hook";

        public final static String EVENT_TYPE_PR = "Merge Request Hook";
    }

    public static class PushAndTagAdapter extends GitHookEventAdapter {

        private class JsonHelper {

            private Repository repository;
        }

        private class Repository {

            private String homepage;
        }

        PushAndTagAdapter(GitSource gitSource, GitEventType eventType) {
            super(gitSource, eventType);
        }

        @Override
        public GitEvent convert(String json) throws GitException {
            GitPushTagEvent event = GSON.fromJson(json, GitPushTagEvent.class);
            JsonHelper jsonHelper = GSON.fromJson(json, JsonHelper.class);

            event.setType(eventType);
            event.setGitSource(gitSource);

            // set head commit url
            final String headCommitUrl = jsonHelper.repository.homepage + "/commit/" + event.getAfter();
            event.setHeadCommitUrl(headCommitUrl);

            // set compare id
            final String compareId = GitPushTagEvent.buildCompareId(event);
            event.setCompareId(compareId);

            // set compare url
            final String compareUrl = jsonHelper.repository.homepage + "/compare/" + compareId;
            event.setCompareUrl(compareUrl);

            return event;
        }
    }

    public static class PullRequestAdaptor extends GitHookEventAdapter {

        public final static String STATE_OPEN = "opened";

        public final static String STATE_CLOSE = "merged";

        public final static String STATE_REOPEN = "reopened";

        private class RequestRoot {

            @SerializedName("user")
            private UserInfo user;

            @SerializedName("object_attributes")
            private ObjectAttributes attrs;
        }

        private class ObjectAttributes {

            private String title;

            private Integer id;

            private String description;

            private String state;

            private String action;

            private String url;

            @SerializedName("author_id")
            private Integer authorId;

            @SerializedName("target_branch")
            private String targetBranch;

            @SerializedName("target_project_id")
            private Integer targetProjectId;

            @SerializedName("source_branch")
            private String sourceBranch;

            @SerializedName("source_project_id")
            private Integer sourceProjectId;

            @SerializedName("last_commit")
            private LastCommit lastCommit;
        }

        private class UserInfo {

            private String name;

            private String username;

            @SerializedName("avatar_url")
            private String avatarUrl;
        }

        private class LastCommit {

            private String id;

            private String message;
        }

        PullRequestAdaptor(GitSource gitSource, GitEventType eventType) {
            super(gitSource, eventType);
        }

        @Override
        public GitEvent convert(String json) throws GitException {
            try {
                RequestRoot requestRoot = GSON.fromJson(json, RequestRoot.class);
                UserInfo user = requestRoot.user;
                ObjectAttributes attrs = requestRoot.attrs;

                GitPullRequestEvent prEvent = new GitPullRequestEvent(gitSource, eventType);

                if (Objects.equals(attrs.state, STATE_OPEN) || Objects.equals(attrs.state, STATE_REOPEN)) {
                    prEvent.setState(State.OPEN);
                    prEvent.setMergedBy("");
                }
                else if (Objects.equals(attrs.state, STATE_CLOSE)) {
                    prEvent.setState(State.CLOSE);
                    prEvent.setMergedBy(user.name);
                }
                else {
                    throw new GitException("The pull request state of GitLab '" + attrs.state + "' not supported");
                }

                prEvent.setTitle(attrs.title);
                prEvent.setRequestId(attrs.id);
                prEvent.setDescription(attrs.description);
                prEvent.setAction(attrs.action);
                prEvent.setUrl(attrs.url);
                prEvent.setSubmitter(user.name);
                prEvent.setUserEmail(user.name); // cannot get user email from pr data
                prEvent.setTarget(new GitPullRequestInfo());
                prEvent.setSource(new GitPullRequestInfo());

                // set pr target info
                GitPullRequestInfo target = prEvent.getTarget();
                target.setBranch(attrs.targetBranch);
                target.setProjectId(attrs.targetProjectId);

                // set pr source info
                GitPullRequestInfo source = prEvent.getSource();
                source.setBranch(attrs.sourceBranch);
                source.setProjectId(attrs.sourceProjectId);

                LastCommit lastCommit = attrs.lastCommit;
                source.setSha(lastCommit.id);

                return prEvent;
            } catch (Throwable e) {
                throw new GitException("Illegal GitLab pull request data", e);
            }
        }
    }
}
