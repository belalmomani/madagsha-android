package discussion.com.paper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import discussion.com.paper.core.AdManager

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // فشل التهيئة لا يعطل اللعبة
        vm.ads = AdManager(StartIoBackend(this), enabled = BuildConfig.STARTIO_APP_ID.isNotBlank()).also { it.start() }
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BackHandler(vm.screen != Screen.HOME) {
                    vm.go(when (vm.screen) { Screen.MODE -> Screen.HOME; Screen.COUNT -> Screen.MODE; Screen.TEAMS -> Screen.COUNT; Screen.AVATAR -> if (vm.teamMode) Screen.TEAMS else Screen.COUNT; else -> Screen.HOME })
                }
                Box(Modifier.fillMaxSize()) { AppRoot(vm) }
            }
        }
    }
}
