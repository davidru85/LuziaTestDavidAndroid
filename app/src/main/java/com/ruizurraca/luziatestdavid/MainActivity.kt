package com.ruizurraca.luziatestdavid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ruizurraca.luziatestdavid.domain.catalog.PersonaCatalog
import com.ruizurraca.luziatestdavid.presentation.screen.ChatScreen
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var personaCatalog: PersonaCatalog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LuziaTheme {
                ChatScreen(personaCatalog = personaCatalog)
            }
        }
    }
}
