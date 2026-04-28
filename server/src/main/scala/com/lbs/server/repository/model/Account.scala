package com.lbs.server.repository.model

import jakarta.persistence.*

@Entity
@Access(AccessType.FIELD)
//just a sequence generator
class Account extends RecordId
