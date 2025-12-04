package com.github.adocker.daemon.registry.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer for empty JSON objects
 */
object EmptyObjectSerializer : KSerializer<Unit> {
    override val descriptor = buildClassSerialDescriptor("EmptyObject")
    override fun serialize(encoder: Encoder, value: Unit) {
        encoder.beginStructure(descriptor).endStructure(descriptor)
    }
    override fun deserialize(decoder: Decoder) {
        decoder.beginStructure(descriptor).endStructure(descriptor)
    }
}