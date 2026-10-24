package io.github.raghavsatyadev.camerabutton.presentation.remote

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import io.github.raghavsatyadev.camerabutton.R
import io.github.raghavsatyadev.camerabutton.presentation.Countdown

/**
 * The big round shutter. Its corners morph while pressed (expressive animated shapes), a ring
 * fills during the self-timer, and [pulse] gives it a small kick each time a photo is sent.
 */
@Composable
fun ShutterButton(
  enabled: Boolean,
  countdown: Countdown?,
  pulse: Animatable<Float, AnimationVector1D>,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // Aim for where the ring should be when the current second ends, so it moves smoothly.
  val target = countdown?.let { 1f - (it.left - 1f) / it.total } ?: 0f
  val progress by
    animateFloatAsState(
      targetValue = target,
      animationSpec = if (countdown == null) snap() else tween(1_000, easing = LinearEasing),
      label = "countdown",
    )

  Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
    if (countdown != null) {
      CircularProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxSize(),
        strokeWidth = 6.dp,
      )
    }
    FilledIconButton(
      onClick = onClick,
      enabled = enabled,
      shapes = IconButtonDefaults.animatedShapes(),
      modifier =
        Modifier.fillMaxSize().padding(10.dp).graphicsLayer {
          scaleX = pulse.value
          scaleY = pulse.value
        },
    ) {
      AnimatedContent(
        targetState = countdown?.left,
        transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
        contentAlignment = Alignment.Center,
        label = "shutterContent",
      ) { left ->
        if (left != null) {
          Text(text = left.toString(), style = MaterialTheme.typography.displayLarge)
        } else {
          Icon(
            painter = painterResource(R.drawable.ic_shutter),
            contentDescription = stringResource(R.string.take_photo),
            modifier = Modifier.fillMaxWidth(0.45f).aspectRatio(1f),
          )
        }
      }
    }
  }
}
