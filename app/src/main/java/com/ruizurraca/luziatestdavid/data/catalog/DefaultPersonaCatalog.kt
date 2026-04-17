package com.ruizurraca.luziatestdavid.data.catalog

import com.ruizurraca.luziatestdavid.domain.catalog.PersonaCatalog
import com.ruizurraca.luziatestdavid.domain.model.Persona
import com.ruizurraca.luziatestdavid.domain.model.PersonaEntry

class DefaultPersonaCatalog(
    private val displayNames: List<String>,
    private val prompts: List<String>
) : PersonaCatalog {

    init {
        require(displayNames.size == Persona.entries.size) {
            "displayNames must have exactly ${Persona.entries.size} entries, got ${displayNames.size}"
        }
        require(prompts.size == Persona.entries.size) {
            "prompts must have exactly ${Persona.entries.size} entries, got ${prompts.size}"
        }
    }

    override fun entries(): List<PersonaEntry> =
        Persona.entries.mapIndexed { index, persona ->
            PersonaEntry(
                persona = persona,
                displayName = displayNames[index],
                prompt = prompts[index]
            )
        }
}
