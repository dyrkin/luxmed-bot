package com.lbs.server.repository.model

import javax.persistence._

@Entity
@Access(AccessType.FIELD)
//just a sequence generator
class Account extends RecordId
