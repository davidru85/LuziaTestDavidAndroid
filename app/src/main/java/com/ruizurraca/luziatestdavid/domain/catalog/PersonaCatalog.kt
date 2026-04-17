package com.ruizurraca.luziatestdavid.domain.catalog

import com.ruizurraca.luziatestdavid.domain.model.PersonaEntry

interface PersonaCatalog {
    fun entries(): List<PersonaEntry>
}
