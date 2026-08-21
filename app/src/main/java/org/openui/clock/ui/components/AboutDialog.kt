package org.openui.clock.ui.components

import androidx.compose.ui.res.stringResource

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.openui.clock.R

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

private val DialogBgColor = Color(0xFF131722)
private val CardBgColor = Color(0xFF1C2234)
private val PillBgColor = Color(0xFF1E2B4D)
private val BrandBlue = Color(0xFF267BFF)
private val LightBlueText = Color(0xFF5A9CFF)
private val IconBgColor = Color(0xFF1B2030)

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .background(DialogBgColor, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Close Button
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(40.dp)
                            .background(IconBgColor, CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(org.openui.clock.R.string.close),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                // Logo
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFF7C4DFF), Color(0xFF267BFF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "UI",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(org.openui.clock.R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val prefix = stringResource(org.openui.clock.R.string.about_by_prefix)
                val brand = stringResource(org.openui.clock.R.string.about_brand_name)
                val subtitle = remember {
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color.White.copy(alpha = 0.6f))) {
                            append(prefix)
                        }
                        withStyle(style = SpanStyle(color = LightBlueText)) {
                            append(brand)
                        }
                    }
                }
                Text(text = subtitle, fontSize = 15.sp)
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Pill
                Box(
                    modifier = Modifier
                        .background(PillBgColor, CircleShape)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(org.openui.clock.R.string.about_version_tag),
                        color = LightBlueText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Text content card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBgColor, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        val textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 22.sp
                        )
                        Text(
                            text = stringResource(org.openui.clock.R.string.about_description_p1),
                            style = textStyle
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(org.openui.clock.R.string.about_description_p2),
                            style = textStyle
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        val desc3 = stringResource(org.openui.clock.R.string.about_description_p3)
                        val bottomText = remember {
                            buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold, color = LightBlueText)) {
                                    append(desc3)
                                }
                                append(" 💙")
                            }
                        }
                        Text(
                            text = bottomText,
                            style = textStyle
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoCard(
                        icon = Icons.Default.Code,
                        text = stringResource(org.openui.clock.R.string.about_open_source),
                        modifier = Modifier.weight(1f)
                    )
                    InfoCard(
                        icon = Icons.Default.Security,
                        text = stringResource(org.openui.clock.R.string.about_no_tracking),
                        modifier = Modifier.weight(1f)
                    )
                    InfoCard(
                        icon = Icons.Default.Favorite,
                        text = stringResource(org.openui.clock.R.string.about_community),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // GitHub Button
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Open-UI-Project/Clock"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(org.openui.clock.R.string.about_github_btn),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Close text button
                Text(
                    text = stringResource(org.openui.clock.R.string.close),
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(CardBgColor, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
