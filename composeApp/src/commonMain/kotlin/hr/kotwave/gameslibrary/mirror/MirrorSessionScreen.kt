package hr.kotwave.gameslibrary.mirror

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.cd_back
import hr.kotwave.gameslibrary.resources.mirror_error_identity_body
import hr.kotwave.gameslibrary.resources.mirror_error_identity_title
import hr.kotwave.gameslibrary.resources.mirror_error_kept_changing_body
import hr.kotwave.gameslibrary.resources.mirror_error_kept_changing_title
import hr.kotwave.gameslibrary.resources.mirror_error_lost_body
import hr.kotwave.gameslibrary.resources.mirror_error_lost_title
import hr.kotwave.gameslibrary.resources.mirror_error_mirror_again
import hr.kotwave.gameslibrary.resources.mirror_error_rejected_body
import hr.kotwave.gameslibrary.resources.mirror_error_rejected_title
import hr.kotwave.gameslibrary.resources.mirror_review_added_here_matched_there
import hr.kotwave.gameslibrary.resources.mirror_review_confirm
import hr.kotwave.gameslibrary.resources.mirror_review_differs_many
import hr.kotwave.gameslibrary.resources.mirror_review_differs_one
import hr.kotwave.gameslibrary.resources.mirror_review_edited_removed
import hr.kotwave.gameslibrary.resources.mirror_review_field_rating
import hr.kotwave.gameslibrary.resources.mirror_review_field_status
import hr.kotwave.gameslibrary.resources.mirror_review_field_wishlist
import hr.kotwave.gameslibrary.resources.mirror_review_footnote
import hr.kotwave.gameslibrary.resources.mirror_review_group_added_twice
import hr.kotwave.gameslibrary.resources.mirror_review_group_added_twice_badge
import hr.kotwave.gameslibrary.resources.mirror_review_group_changed
import hr.kotwave.gameslibrary.resources.mirror_review_group_changed_badge
import hr.kotwave.gameslibrary.resources.mirror_review_group_deleted
import hr.kotwave.gameslibrary.resources.mirror_review_group_deleted_badge
import hr.kotwave.gameslibrary.resources.mirror_review_igdb_match
import hr.kotwave.gameslibrary.resources.mirror_review_keep_both
import hr.kotwave.gameslibrary.resources.mirror_review_matched_here_added_there
import hr.kotwave.gameslibrary.resources.mirror_review_merge
import hr.kotwave.gameslibrary.resources.mirror_review_not_wishlist
import hr.kotwave.gameslibrary.resources.mirror_review_on_wishlist
import hr.kotwave.gameslibrary.resources.mirror_review_other_device
import hr.kotwave.gameslibrary.resources.mirror_review_pill_added_twice
import hr.kotwave.gameslibrary.resources.mirror_review_pill_conflicts
import hr.kotwave.gameslibrary.resources.mirror_review_pill_decided
import hr.kotwave.gameslibrary.resources.mirror_review_pill_decided_count
import hr.kotwave.gameslibrary.resources.mirror_review_rating_chip
import hr.kotwave.gameslibrary.resources.mirror_review_removed_edited
import hr.kotwave.gameslibrary.resources.mirror_review_removed_here
import hr.kotwave.gameslibrary.resources.mirror_review_removed_there
import hr.kotwave.gameslibrary.resources.mirror_review_side_manual
import hr.kotwave.gameslibrary.resources.mirror_review_side_matched
import hr.kotwave.gameslibrary.resources.mirror_review_this_device
import hr.kotwave.gameslibrary.resources.mirror_review_title
import hr.kotwave.gameslibrary.resources.mirror_session_cancel_footnote
import hr.kotwave.gameslibrary.resources.mirror_session_compare_clean
import hr.kotwave.gameslibrary.resources.mirror_session_compare_rows
import hr.kotwave.gameslibrary.resources.mirror_session_infoband
import hr.kotwave.gameslibrary.resources.mirror_session_open_review
import hr.kotwave.gameslibrary.resources.mirror_session_pull_attempt
import hr.kotwave.gameslibrary.resources.mirror_session_pulled_count
import hr.kotwave.gameslibrary.resources.mirror_session_review_kept
import hr.kotwave.gameslibrary.resources.mirror_session_review_skipped
import hr.kotwave.gameslibrary.resources.mirror_session_review_waiting
import hr.kotwave.gameslibrary.resources.mirror_session_step_apply
import hr.kotwave.gameslibrary.resources.mirror_session_step_compare
import hr.kotwave.gameslibrary.resources.mirror_session_step_compare_done
import hr.kotwave.gameslibrary.resources.mirror_session_step_pull
import hr.kotwave.gameslibrary.resources.mirror_session_step_pull_again
import hr.kotwave.gameslibrary.resources.mirror_session_step_pull_done
import hr.kotwave.gameslibrary.resources.mirror_session_step_review
import hr.kotwave.gameslibrary.resources.mirror_session_step_send
import hr.kotwave.gameslibrary.resources.mirror_session_step_send_live
import hr.kotwave.gameslibrary.resources.mirror_session_title
import hr.kotwave.gameslibrary.resources.mirror_summary_added
import hr.kotwave.gameslibrary.resources.mirror_summary_body
import hr.kotwave.gameslibrary.resources.mirror_summary_done
import hr.kotwave.gameslibrary.resources.mirror_summary_first_footnote
import hr.kotwave.gameslibrary.resources.mirror_summary_footnote
import hr.kotwave.gameslibrary.resources.mirror_summary_matching_body
import hr.kotwave.gameslibrary.resources.mirror_summary_matching_title
import hr.kotwave.gameslibrary.resources.mirror_summary_removed
import hr.kotwave.gameslibrary.resources.mirror_summary_side_mine
import hr.kotwave.gameslibrary.resources.mirror_summary_side_theirs
import hr.kotwave.gameslibrary.resources.mirror_summary_time_date
import hr.kotwave.gameslibrary.resources.mirror_summary_time_today
import hr.kotwave.gameslibrary.resources.mirror_summary_title
import hr.kotwave.gameslibrary.resources.mirror_summary_updated
import hr.kotwave.gameslibrary.transfer.ExportedGame
import hr.kotwave.gameslibrary.ui.components.CoverArt
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.components.SecondaryButton
import hr.kotwave.gameslibrary.ui.components.StatusDot
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.model.label
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val OkGreen = Color(0xFF7DF0B6)
private val WarnAmber = Color(0xFFFFD24A)
private val ErrorRed = Color(0xFFF4707A)
private const val MAX_ATTEMPTS = 3

/** The phone's Mirror session: step checklist (V1–V7), Conflict Review, and the summary. */
@Composable
fun MirrorSessionScreen(onExit: () -> Unit) {
    val viewModel: MirrorSessionViewModel = koinViewModel()
    LaunchedEffect(Unit) { viewModel.start() }

    when (val phase = viewModel.phase) {
        is MirrorSessionPhase.Steps -> StepsContent(
            steps = phase,
            onOpenReview = viewModel::openReview,
            onCancel = {
                viewModel.cancel()
                onExit()
            },
        )

        is MirrorSessionPhase.Review -> ReviewContent(phase, viewModel)
        is MirrorSessionPhase.Summary -> SummaryContent(phase, onDone = onExit)
        is MirrorSessionPhase.Failure -> FailureContent(phase, onRetry = viewModel::retry, onDone = onExit)
    }
}

@Composable
private fun StepsContent(steps: MirrorSessionPhase.Steps, onOpenReview: () -> Unit, onCancel: () -> Unit) {
    val tokens = AppTheme.tokens
    val sending = steps.current >= MirrorStep.SEND
    Column(Modifier.fillMaxSize().padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md)) {
        SessionHeader(stringResource(Res.string.mirror_session_title), onBack = onCancel.takeUnless { sending })
        Spacer(Modifier.height(tokens.spacing.md))

        val cardShape = RoundedCornerShape(tokens.radii.lg)
        Column(
            Modifier.fillMaxWidth().clip(cardShape)
                .background(tokens.colors.surface).border(1.dp, tokens.colors.border, cardShape)
                .padding(tokens.spacing.md),
        ) {
            PullRow(steps)
            CompareRow(steps)
            ReviewRow(steps)
            StepRow(
                state = stepState(MirrorStep.SEND, steps),
                title = stringResource(
                    if (steps.current == MirrorStep.SEND) Res.string.mirror_session_step_send_live
                    else Res.string.mirror_session_step_send,
                ),
            )
            StepRow(state = stepState(MirrorStep.APPLY, steps), title = stringResource(Res.string.mirror_session_step_apply))
        }

        Spacer(Modifier.height(tokens.spacing.md))
        InfoBand(stringResource(Res.string.mirror_session_infoband))

        if (steps.awaitingReview) {
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = pluralStringResource(
                    Res.plurals.mirror_session_open_review,
                    steps.rowsToDecide ?: 0,
                    steps.rowsToDecide ?: 0,
                ),
                onClick = onOpenReview,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(tokens.spacing.sm))
            Text(
                stringResource(Res.string.mirror_session_cancel_footnote),
                style = AppTheme.type.caption,
                color = tokens.colors.faint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PullRow(steps: MirrorSessionPhase.Steps) {
    val live = steps.current == MirrorStep.PULL
    StepRow(
        state = stepState(MirrorStep.PULL, steps),
        title = stringResource(
            when {
                live && steps.attempt > 1 -> Res.string.mirror_session_step_pull_again
                live -> Res.string.mirror_session_step_pull
                else -> Res.string.mirror_session_step_pull_done
            },
        ),
        subtitle = when {
            live && steps.attempt > 1 ->
                stringResource(Res.string.mirror_session_pull_attempt, steps.attempt, MAX_ATTEMPTS)

            steps.pulledCount != null ->
                pluralStringResource(Res.plurals.mirror_session_pulled_count, steps.pulledCount, steps.pulledCount)

            else -> null
        },
        warnSubtitle = live && steps.attempt > 1,
    )
}

@Composable
private fun CompareRow(steps: MirrorSessionPhase.Steps) {
    val done = steps.current > MirrorStep.COMPARE
    StepRow(
        state = stepState(MirrorStep.COMPARE, steps),
        title = stringResource(
            if (done) Res.string.mirror_session_step_compare_done else Res.string.mirror_session_step_compare,
        ),
        subtitle = steps.rowsToDecide?.let { rows ->
            if (rows == 0) {
                stringResource(Res.string.mirror_session_compare_clean)
            } else {
                pluralStringResource(Res.plurals.mirror_session_compare_rows, rows, rows)
            }
        },
    )
}

@Composable
private fun ReviewRow(steps: MirrorSessionPhase.Steps) {
    val skipped = steps.rowsToDecide == 0 && steps.current > MirrorStep.REVIEW
    StepRow(
        state = if (skipped) StepState.SKIPPED else stepState(MirrorStep.REVIEW, steps),
        title = stringResource(
            if (skipped) Res.string.mirror_session_review_skipped else Res.string.mirror_session_step_review,
        ),
        subtitle = when {
            steps.awaitingReview -> stringResource(Res.string.mirror_session_review_waiting)
            steps.attempt > 1 && steps.current <= MirrorStep.REVIEW ->
                stringResource(Res.string.mirror_session_review_kept)

            else -> null
        },
    )
}

private enum class StepState { DONE, LIVE, WAIT, SKIPPED }

private fun stepState(step: MirrorStep, steps: MirrorSessionPhase.Steps): StepState = when {
    step < steps.current -> StepState.DONE
    step == steps.current -> StepState.LIVE
    else -> StepState.WAIT
}

@Composable
private fun StepRow(state: StepState, title: String, subtitle: String? = null, warnSubtitle: Boolean = false) {
    val tokens = AppTheme.tokens
    Row(
        Modifier.fillMaxWidth().padding(vertical = tokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            when (state) {
                StepState.DONE -> Box(
                    Modifier.size(26.dp).clip(CircleShape)
                        .background(OkGreen.copy(alpha = 0.12f)).border(1.dp, OkGreen.copy(alpha = 0.40f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Check, null, Modifier.size(13.dp), tint = OkGreen)
                }

                StepState.LIVE -> CircularProgressIndicator(
                    Modifier.size(22.dp),
                    color = tokens.colors.accent,
                    strokeWidth = 2.5.dp,
                )

                StepState.WAIT -> Box(
                    Modifier.size(26.dp).clip(CircleShape)
                        .border(1.5.dp, tokens.colors.borderStrong, CircleShape),
                )

                StepState.SKIPPED -> Box(
                    Modifier.size(26.dp).clip(CircleShape)
                        .background(tokens.colors.surfaceRaised).border(1.dp, tokens.colors.border, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Minus, null, Modifier.size(12.dp), tint = tokens.colors.faint)
                }
            }
        }
        Column {
            Text(
                title,
                style = AppTheme.type.bodyStrong,
                color = when (state) {
                    StepState.WAIT -> tokens.colors.faint
                    StepState.SKIPPED -> tokens.colors.muted
                    else -> tokens.colors.text
                },
            )
            subtitle?.let {
                Text(it, style = AppTheme.type.caption, color = if (warnSubtitle) WarnAmber else tokens.colors.faint)
            }
        }
    }
}

@Composable
private fun InfoBand(text: String) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(tokens.colors.accent.copy(alpha = 0.06f))
            .border(1.dp, tokens.colors.accent.copy(alpha = 0.28f), shape)
            .padding(tokens.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(tokens.radii.sm))
                .background(tokens.colors.accent.copy(alpha = 0.14f))
                .border(1.dp, tokens.colors.accent.copy(alpha = 0.35f), RoundedCornerShape(tokens.radii.sm)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Mirror, null, Modifier.size(19.dp), tint = tokens.colors.accent)
        }
        Text(text, style = AppTheme.type.body.copy(fontSize = 12.5.sp), color = tokens.colors.muted)
    }
}

@Composable
private fun ReviewContent(review: MirrorSessionPhase.Review, viewModel: MirrorSessionViewModel) {
    val tokens = AppTheme.tokens
    val total = review.conflicts.size + review.collisions.size
    val decided = review.conflicts.count { it in viewModel.conflictChoices } +
        review.collisions.count { it in viewModel.collisionChoices }
    val userState = review.conflicts.filter { it.kind == MirrorConflictKind.UserState }
    val deleteVsEdit = review.conflicts.filter { it.kind == MirrorConflictKind.DeleteVsEdit }

    Column(Modifier.fillMaxSize().padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md)) {
        SessionHeader(stringResource(Res.string.mirror_review_title), onBack = viewModel::backFromReview)
        Spacer(Modifier.height(tokens.spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs), modifier = Modifier.fillMaxWidth()) {
            ReviewPill(review.conflicts.size.toString(), stringResource(Res.string.mirror_review_pill_conflicts), WarnAmber, Modifier.weight(1f))
            ReviewPill(review.collisions.size.toString(), stringResource(Res.string.mirror_review_pill_added_twice), tokens.colors.accent, Modifier.weight(1f))
            ReviewPill(
                stringResource(Res.string.mirror_review_pill_decided_count, decided, total),
                stringResource(Res.string.mirror_review_pill_decided),
                OkGreen,
                Modifier.weight(1f),
            )
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            if (userState.isNotEmpty()) {
                item {
                    GroupHeader(
                        stringResource(Res.string.mirror_review_group_changed),
                        stringResource(Res.string.mirror_review_group_changed_badge),
                        WarnAmber,
                    )
                }
                items(userState) { conflict ->
                    ConflictCard(conflict, viewModel.conflictChoices[conflict]) { keepMine ->
                        viewModel.chooseConflict(conflict, keepMine)
                    }
                }
            }
            if (deleteVsEdit.isNotEmpty()) {
                item {
                    GroupHeader(
                        stringResource(Res.string.mirror_review_group_deleted),
                        stringResource(Res.string.mirror_review_group_deleted_badge),
                        ErrorRed,
                    )
                }
                items(deleteVsEdit) { conflict ->
                    ConflictCard(conflict, viewModel.conflictChoices[conflict]) { keepMine ->
                        viewModel.chooseConflict(conflict, keepMine)
                    }
                }
            }
            if (review.collisions.isNotEmpty()) {
                item {
                    GroupHeader(
                        stringResource(Res.string.mirror_review_group_added_twice),
                        stringResource(Res.string.mirror_review_group_added_twice_badge),
                        tokens.colors.accent,
                    )
                }
                items(review.collisions) { collision ->
                    CollisionCard(collision, viewModel.collisionChoices[collision]) { merge ->
                        viewModel.chooseCollision(collision, merge)
                    }
                }
            }
        }

        Spacer(Modifier.height(tokens.spacing.sm))
        PrimaryButton(
            text = stringResource(Res.string.mirror_review_confirm),
            onClick = viewModel::confirmReview,
            leadingIcon = AppIcons.Check,
            enabled = decided == total,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(tokens.spacing.xs))
        Text(
            stringResource(Res.string.mirror_review_footnote, decided, total),
            style = AppTheme.type.caption,
            color = tokens.colors.faint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ReviewPill(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Column(
        modifier.clip(shape).background(tokens.colors.surface).border(1.dp, tokens.colors.border, shape)
            .padding(vertical = tokens.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = AppTheme.type.bodyStrong.copy(fontSize = 17.sp), color = color)
        Text(label, style = AppTheme.type.caption.copy(fontSize = 10.sp), color = tokens.colors.faint)
    }
}

@Composable
private fun GroupHeader(title: String, badge: String, color: Color) {
    val tokens = AppTheme.tokens
    Row(
        Modifier.padding(top = tokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        Text(title.uppercase(), style = AppTheme.type.caption.copy(fontSize = 11.sp), color = tokens.colors.faint)
        Box(
            Modifier.clip(RoundedCornerShape(tokens.radii.sm)).background(color.copy(alpha = 0.12f))
                .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            Text(badge, style = AppTheme.type.caption.copy(fontSize = 10.sp), color = color)
        }
    }
}

/** One Conflict row: side-by-side "This device / Other device", tap-to-keep. */
@Composable
private fun ConflictCard(conflict: MirrorConflict, choice: Boolean?, onChoose: (Boolean) -> Unit) {
    val tokens = AppTheme.tokens
    val shown = conflict.merged ?: conflict.mine ?: conflict.theirs ?: return
    val shape = RoundedCornerShape(tokens.radii.md)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(tokens.colors.surface)
            .border(1.dp, tokens.colors.border, shape).padding(tokens.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        RowHead(shown, conflictSubtitle(conflict))
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            ConflictSide(
                game = conflict.mine,
                fields = conflict.fields,
                label = stringResource(Res.string.mirror_review_this_device),
                goneLabel = stringResource(Res.string.mirror_review_removed_here),
                selected = choice == true,
                modifier = Modifier.weight(1f),
            ) { onChoose(true) }
            ConflictSide(
                game = conflict.theirs,
                fields = conflict.fields,
                label = stringResource(Res.string.mirror_review_other_device),
                goneLabel = stringResource(Res.string.mirror_review_removed_there),
                selected = choice == false,
                modifier = Modifier.weight(1f),
            ) { onChoose(false) }
        }
    }
}

@Composable
private fun conflictSubtitle(conflict: MirrorConflict): String {
    if (conflict.kind == MirrorConflictKind.DeleteVsEdit) {
        return stringResource(
            if (conflict.mine == null) Res.string.mirror_review_removed_edited
            else Res.string.mirror_review_edited_removed,
        )
    }
    val names = listOfNotNull(
        stringResource(Res.string.mirror_review_field_status).takeIf { MirrorField.STATUS in conflict.fields },
        stringResource(Res.string.mirror_review_field_rating).takeIf { MirrorField.USER_RATING in conflict.fields },
        stringResource(Res.string.mirror_review_field_wishlist).takeIf { MirrorField.WISHLIST in conflict.fields },
    )
    val joined = names.mapIndexed { index, name ->
        if (index == 0) name else name.replaceFirstChar { it.lowercase() }
    }.joinToString(" + ")
    return stringResource(
        if (names.size == 1) Res.string.mirror_review_differs_one else Res.string.mirror_review_differs_many,
        joined,
    )
}

@Composable
private fun RowHead(game: ExportedGame, subtitle: String) {
    val tokens = AppTheme.tokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        CoverArt(game.name, game.coverImageId, Modifier.size(width = 38.dp, height = 50.dp), RoundedCornerShape(tokens.radii.sm))
        Column {
            Text(game.name, style = AppTheme.type.bodyStrong, color = tokens.colors.text)
            Text(subtitle.uppercase(), style = AppTheme.type.caption.copy(fontSize = 10.sp), color = tokens.colors.faint)
        }
    }
}

@Composable
private fun ConflictSide(
    game: ExportedGame?,
    fields: Set<MirrorField>,
    label: String,
    goneLabel: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
) {
    val tokens = AppTheme.tokens
    val gone = game == null
    val accent = if (gone) ErrorRed else tokens.colors.accent
    val shape = RoundedCornerShape(tokens.radii.sm)
    Column(
        modifier.clip(shape)
            .background(if (selected) accent.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f))
            .border(1.5.dp, if (selected) accent.copy(alpha = 0.65f) else tokens.colors.border, shape)
            .clickable(onClick = onSelect)
            .padding(tokens.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            Text(
                label.uppercase(),
                style = AppTheme.type.caption.copy(fontSize = 9.5.sp),
                color = if (gone) ErrorRed.copy(alpha = 0.8f) else tokens.colors.faint,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.size(18.dp).clip(CircleShape)
                    .background(if (selected) accent else Color.Transparent)
                    .border(1.5.dp, if (selected) accent else tokens.colors.borderStrong, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(AppIcons.Check, null, Modifier.size(10.dp), tint = Color.White)
            }
        }
        if (game == null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                Icon(AppIcons.Trash, null, Modifier.size(14.dp), tint = ErrorRed)
                Text(goneLabel, style = AppTheme.type.caption, color = ErrorRed)
            }
        } else {
            SideChips(game, fields)
        }
    }
}

/** The chips a Conflict side shows: only the conflicting [fields] for a UserState row, else user state. */
@Composable
private fun SideChips(game: ExportedGame, fields: Set<MirrorField>) {
    val tokens = AppTheme.tokens
    val showStatus = if (fields.isEmpty()) game.status != null else MirrorField.STATUS in fields
    val showRating = if (fields.isEmpty()) game.userRating != null else MirrorField.USER_RATING in fields
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
        if (showStatus) {
            val status = game.status?.let { raw -> Status.entries.firstOrNull { it.name == raw } }
            Chip {
                if (status != null) {
                    StatusDot(status, size = 8.dp, bordered = false)
                    Text(status.label(), style = AppTheme.type.caption, color = tokens.colors.muted)
                } else {
                    Text("—", style = AppTheme.type.caption, color = tokens.colors.faint)
                }
            }
        }
        if (showRating) {
            Chip {
                Icon(AppIcons.Star, null, Modifier.size(11.dp), tint = tokens.colors.accent)
                Text(
                    if (game.userRating != null) {
                        stringResource(Res.string.mirror_review_rating_chip, ratingLabel(game.userRating!!))
                    } else {
                        "—"
                    },
                    style = AppTheme.type.caption,
                    color = tokens.colors.muted,
                )
            }
        }
        if (MirrorField.WISHLIST in fields) {
            Chip {
                Text(
                    stringResource(
                        if (game.wishlist) Res.string.mirror_review_on_wishlist else Res.string.mirror_review_not_wishlist,
                    ),
                    style = AppTheme.type.caption,
                    color = tokens.colors.muted,
                )
            }
        }
    }
}

@Composable
private fun Chip(content: @Composable () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.sm)
    Row(
        Modifier.clip(shape).background(tokens.colors.surfaceRaised).border(1.dp, tokens.colors.border, shape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}

private fun ratingLabel(rating: Double): String =
    if (rating % 1.0 == 0.0) rating.toInt().toString() else rating.toString()

/** One collision row: manual vs matched panels plus the merge / keep-both segment. */
@Composable
private fun CollisionCard(collision: MirrorCollision, choice: Boolean?, onChoose: (Boolean) -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(tokens.colors.surface)
            .border(1.dp, tokens.colors.border, shape).padding(tokens.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        RowHead(
            collision.matched,
            stringResource(
                if (collision.manualIsMine) Res.string.mirror_review_added_here_matched_there
                else Res.string.mirror_review_matched_here_added_there,
            ),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            val mineGame = if (collision.manualIsMine) collision.manual else collision.matched
            val theirsGame = if (collision.manualIsMine) collision.matched else collision.manual
            CollisionSide(
                game = mineGame,
                manual = collision.manualIsMine,
                label = stringResource(Res.string.mirror_review_this_device),
                modifier = Modifier.weight(1f),
            )
            CollisionSide(
                game = theirsGame,
                manual = !collision.manualIsMine,
                label = stringResource(Res.string.mirror_review_other_device),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            SegmentOption(stringResource(Res.string.mirror_review_merge), choice == true, Modifier.weight(1f)) { onChoose(true) }
            SegmentOption(stringResource(Res.string.mirror_review_keep_both), choice == false, Modifier.weight(1f)) { onChoose(false) }
        }
    }
}

@Composable
private fun CollisionSide(game: ExportedGame, manual: Boolean, label: String, modifier: Modifier = Modifier) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.sm)
    val store = game.ownerships.firstOrNull()?.store
        ?.let { raw -> Store.entries.firstOrNull { it.name == raw }?.label ?: raw }
    Column(
        modifier.clip(shape).background(Color.Black.copy(alpha = 0.22f)).border(1.dp, tokens.colors.border, shape)
            .padding(tokens.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        Text(
            stringResource(
                if (manual) Res.string.mirror_review_side_manual else Res.string.mirror_review_side_matched,
                label,
            ).uppercase(),
            style = AppTheme.type.caption.copy(fontSize = 9.5.sp),
            color = tokens.colors.faint,
        )
        Chip {
            Text(
                if (manual) "“${game.name}”" else stringResource(Res.string.mirror_review_igdb_match),
                style = AppTheme.type.caption,
                color = if (manual) tokens.colors.muted else OkGreen,
            )
        }
        store?.let { Chip { Text(it, style = AppTheme.type.caption, color = tokens.colors.muted) } }
    }
}

@Composable
private fun SegmentOption(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.sm)
    Box(
        modifier.clip(shape)
            .background(if (selected) tokens.colors.accent.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.22f))
            .border(1.5.dp, if (selected) tokens.colors.accent.copy(alpha = 0.6f) else tokens.colors.border, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = AppTheme.type.caption.copy(fontSize = 12.sp),
            color = if (selected) tokens.colors.text else tokens.colors.muted,
        )
    }
}

@Composable
private fun SummaryContent(summary: MirrorSessionPhase.Summary, onDone: () -> Unit) {
    val tokens = AppTheme.tokens
    Column(Modifier.fillMaxSize().padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md)) {
        Column(
            Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape)
                    .background(OkGreen.copy(alpha = 0.12f)).border(1.dp, OkGreen.copy(alpha = 0.40f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Check, null, Modifier.size(34.dp), tint = OkGreen)
            }
            Spacer(Modifier.height(tokens.spacing.lg))
            if (summary.nothingChanged) {
                Text(
                    stringResource(Res.string.mirror_summary_matching_title),
                    style = AppTheme.type.display,
                    color = tokens.colors.text,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(tokens.spacing.sm))
                Text(
                    stringResource(Res.string.mirror_summary_matching_body),
                    style = AppTheme.type.body,
                    color = tokens.colors.muted,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    stringResource(Res.string.mirror_summary_title),
                    style = AppTheme.type.display,
                    color = tokens.colors.text,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(tokens.spacing.sm))
                Text(
                    pluralStringResource(
                        Res.plurals.mirror_summary_body,
                        summary.gameCount,
                        summary.gameCount,
                        finishedAtLabel(summary.finishedAtMillis),
                    ),
                    style = AppTheme.type.body,
                    color = tokens.colors.muted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(tokens.spacing.lg))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                    SummarySide(
                        stringResource(Res.string.mirror_summary_side_mine),
                        summary.mine,
                        summary.firstMirror,
                        Modifier.weight(1f),
                    )
                    SummarySide(
                        stringResource(Res.string.mirror_summary_side_theirs),
                        summary.theirs,
                        summary.firstMirror,
                        Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(tokens.spacing.md))
                Text(
                    stringResource(
                        if (summary.firstMirror) Res.string.mirror_summary_first_footnote
                        else Res.string.mirror_summary_footnote,
                    ),
                    style = AppTheme.type.caption,
                    color = tokens.colors.faint,
                    textAlign = TextAlign.Center,
                )
            }
        }
        PrimaryButton(
            text = stringResource(Res.string.mirror_summary_done),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun finishedAtLabel(epochMillis: Long): String {
    val (date, time) = mirrorTimeParts(epochMillis, epochMillis)
    return if (date == null) {
        stringResource(Res.string.mirror_summary_time_today, time)
    } else {
        stringResource(Res.string.mirror_summary_time_date, date, time)
    }
}

@Composable
private fun SummarySide(
    label: String,
    counts: MirrorSessionPhase.SideCounts,
    firstMirror: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Column(
        modifier.clip(shape).background(tokens.colors.surface).border(1.dp, tokens.colors.border, shape)
            .padding(tokens.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        Text(label.uppercase(), style = AppTheme.type.caption.copy(fontSize = 10.sp), color = tokens.colors.faint)
        SummaryLine(stringResource(Res.string.mirror_summary_added), "+${counts.added}", OkGreen)
        SummaryLine(stringResource(Res.string.mirror_summary_updated), counts.updated.toString(), AppTheme.tokens.colors.accent)
        if (!firstMirror) {
            SummaryLine(stringResource(Res.string.mirror_summary_removed), counts.removed.toString(), ErrorRed)
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, color: Color) {
    val tokens = AppTheme.tokens
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = AppTheme.type.caption, color = tokens.colors.muted)
        Text(value, style = AppTheme.type.bodyStrong.copy(fontSize = 14.sp), color = color)
    }
}

@Composable
private fun FailureContent(failure: MirrorSessionPhase.Failure, onRetry: () -> Unit, onDone: () -> Unit) {
    val tokens = AppTheme.tokens
    val retryable = failure is MirrorSessionPhase.Failure.HostKeptChanging ||
        failure is MirrorSessionPhase.Failure.ConnectionLost
    Column(Modifier.fillMaxSize().padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md)) {
        Column(
            Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape)
                    .background(WarnAmber.copy(alpha = 0.12f)).border(1.dp, WarnAmber.copy(alpha = 0.40f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Close, null, Modifier.size(30.dp), tint = WarnAmber)
            }
            Spacer(Modifier.height(tokens.spacing.lg))
            Text(
                stringResource(
                    when (failure) {
                        MirrorSessionPhase.Failure.HostKeptChanging -> Res.string.mirror_error_kept_changing_title
                        MirrorSessionPhase.Failure.ConnectionLost -> Res.string.mirror_error_lost_title
                        MirrorSessionPhase.Failure.PairingRejected -> Res.string.mirror_error_rejected_title
                        MirrorSessionPhase.Failure.HostIdentityChanged -> Res.string.mirror_error_identity_title
                    },
                ),
                style = AppTheme.type.display,
                color = tokens.colors.text,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(tokens.spacing.sm))
            Text(
                stringResource(
                    when (failure) {
                        MirrorSessionPhase.Failure.HostKeptChanging -> Res.string.mirror_error_kept_changing_body
                        MirrorSessionPhase.Failure.ConnectionLost -> Res.string.mirror_error_lost_body
                        MirrorSessionPhase.Failure.PairingRejected -> Res.string.mirror_error_rejected_body
                        MirrorSessionPhase.Failure.HostIdentityChanged -> Res.string.mirror_error_identity_body
                    },
                ),
                style = AppTheme.type.body,
                color = tokens.colors.muted,
                textAlign = TextAlign.Center,
            )
        }
        if (retryable) {
            PrimaryButton(
                text = stringResource(Res.string.mirror_error_mirror_again),
                onClick = onRetry,
                leadingIcon = AppIcons.Sync,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(tokens.spacing.sm))
        }
        SecondaryButton(
            text = stringResource(Res.string.mirror_summary_done),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SessionHeader(title: String, onBack: (() -> Unit)?) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(tokens.radii.md)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        if (onBack != null) {
            Box(
                Modifier.size(36.dp).clip(shape)
                    .background(tokens.colors.surface).border(1.dp, tokens.colors.border, shape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.ChevronLeft, stringResource(Res.string.cd_back), Modifier.size(18.dp), tint = tokens.colors.muted)
            }
        }
        Text(title, style = AppTheme.type.brand.copy(fontSize = 16.sp), color = tokens.colors.text)
    }
}
