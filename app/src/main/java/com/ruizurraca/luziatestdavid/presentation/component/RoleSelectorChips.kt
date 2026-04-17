package com.ruizurraca.luziatestdavid.presentation.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruizurraca.luziatestdavid.domain.model.Persona
import com.ruizurraca.luziatestdavid.domain.model.PersonaEntry
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme

@Composable
fun RoleSelectorChips(
    entries: List<PersonaEntry>,
    selectedPersona: Persona,
    onPersonaSelected: (Persona) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(items = entries, key = { it.persona }) { entry ->
            val selected = entry.persona == selectedPersona
            FilterChip(
                selected = selected,
                onClick = { onPersonaSelected(entry.persona) },
                label = { Text(entry.displayName) },
                leadingIcon = if (selected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                modifier = Modifier.semantics { role = Role.RadioButton }
            )
        }
    }
}

private fun previewPersonaEntries(): List<PersonaEntry> = listOf(
    PersonaEntry(Persona.STUDENT, "Student", "tutor prompt"),
    PersonaEntry(Persona.SCIENTIST, "Scientist", "scientist prompt"),
    PersonaEntry(Persona.ARTIST, "Artist", "artist prompt")
)

@Preview(showBackground = true, name = "Light — Student selected")
@Composable
private fun RoleSelectorChipsPreview_StudentSelected() {
    LuziaTheme {
        RoleSelectorChips(
            entries = previewPersonaEntries(),
            selectedPersona = Persona.STUDENT,
            onPersonaSelected = {}
        )
    }
}

@Preview(showBackground = true, name = "Light — Artist selected")
@Composable
private fun RoleSelectorChipsPreview_ArtistSelected() {
    LuziaTheme {
        RoleSelectorChips(
            entries = previewPersonaEntries(),
            selectedPersona = Persona.ARTIST,
            onPersonaSelected = {}
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark — Student selected"
)
@Composable
private fun RoleSelectorChipsPreview_DarkStudent() {
    LuziaTheme {
        RoleSelectorChips(
            entries = previewPersonaEntries(),
            selectedPersona = Persona.STUDENT,
            onPersonaSelected = {}
        )
    }
}
