package io.xauth.actor

import io.xauth.service.workspace.model.Workspace

/**
 * Basic actor message that supply the workspace.
 */
abstract class WorkspaceMessage(implicit workspace: Workspace)
