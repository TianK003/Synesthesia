package dev.tiank003.synesthesia.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tiank003.synesthesia.core.dsp.FFTProcessor
import dev.tiank003.synesthesia.core.dsp.WindowFunction
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    private const val DEFAULT_FFT_SIZE = 2048

    @Provides
    @Singleton
    fun provideFFTProcessor(): FFTProcessor = FFTProcessor(DEFAULT_FFT_SIZE)

    @Provides
    @Singleton
    fun provideWindowFunction(): WindowFunction = WindowFunction.Hann(DEFAULT_FFT_SIZE)
}
