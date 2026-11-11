package com.creditlens.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.creditlens.app.ui.navigation.CreditLensNavHost
import com.creditlens.app.ui.theme.CreditLensTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as CreditLensApplication

        setContent {
            CreditLensTheme {
                CreditLensNavHost(repository = app.repository)
            }
        }
    }
}
