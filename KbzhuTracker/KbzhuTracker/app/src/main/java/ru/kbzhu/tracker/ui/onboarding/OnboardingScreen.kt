package ru.kbzhu.tracker.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.ui.components.PrimaryButton

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onClose: (() -> Unit)? = null,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OnboardingEvent.Finished -> onFinished()
            }
        }
    }

    // Системная «назад» листает шаги; на первом шаге в режиме редактирования — закрывает экран
    BackHandler(enabled = !state.isFirstStep || onClose != null) {
        if (!viewModel.back()) onClose?.invoke()
    }

    val progress by animateFloatAsState(state.progress, tween(400), label = "onboardingProgress")

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // ---------- Верх: назад + прогресс ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    when {
                        !state.isFirstStep -> IconButton(onClick = { viewModel.back() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                        }
                        onClose != null -> IconButton(onClick = onClose) {
                            Icon(Icons.Filled.Close, stringResource(R.string.action_close))
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
                Text(
                    text = stringResource(R.string.onb_step_of, state.stepIndex + 1, state.visibleSteps.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp)
                )
            }

            // ---------- Шаг ----------
            AnimatedContent(
                targetState = state.step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally(tween(300)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(tween(300)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(200)))
                },
                label = "onboardingStep"
            ) { step ->
                when (step) {
                    OnboardingStep.GENDER -> GenderStep(state, viewModel::selectGender)
                    OnboardingStep.BODY -> BodyStep(
                        state,
                        onAge = viewModel::onAgeChange,
                        onHeight = viewModel::onHeightChange,
                        onWeight = viewModel::onWeightChange,
                        onDone = viewModel::next
                    )
                    OnboardingStep.GOAL -> GoalStep(state, viewModel::selectGoal)
                    OnboardingStep.TARGET -> TargetStep(state, viewModel::onTargetWeightChange, viewModel::selectRate)
                    OnboardingStep.ACTIVITY -> ActivityStep(state, viewModel::selectActivity)
                    OnboardingStep.MACROS -> MacrosStep(
                        state,
                        onPreset = viewModel::selectPreset,
                        onChange = viewModel::changeMacro,
                        previewGrams = { macro -> viewModel.previewGrams(macro, state) }
                    )
                    OnboardingStep.RESULT -> ResultStep(
                        state,
                        onManualToggle = viewModel::setManualEnabled,
                        onCalories = viewModel::onManualCaloriesChange,
                        onMacro = viewModel::onManualMacroChange
                    )
                }
            }

            // ---------- Низ: большая кнопка в зоне большого пальца ----------
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = stringResource(
                    when {
                        !state.isLastStep -> R.string.action_next
                        state.editMode -> R.string.action_save
                        else -> R.string.onb_start
                    }
                ),
                onClick = viewModel::next,
                enabled = state.canProceed,
                loading = state.isSaving,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}
