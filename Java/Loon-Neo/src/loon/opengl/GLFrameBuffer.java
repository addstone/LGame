/**
 * Copyright 2008 - 2019 The Loon Game Engine Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * @project loon
 * @author cping
 * @email：javachenpeng@yahoo.com
 * @version 0.5
 */
package loon.opengl;

import static loon.opengl.GL20.GL_COLOR_ATTACHMENT0;
import static loon.opengl.GL20.GL_FRAMEBUFFER;
import static loon.opengl.GL20.GL_TEXTURE_2D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import loon.LGame;
import loon.LRelease;
import loon.LSysException;
import loon.LSystem;
import loon.LTexture;
import loon.canvas.Image;
import loon.utils.GLUtils;
import loon.utils.TArray;

public abstract class GLFrameBuffer implements LRelease {

	public enum FrameBufferDepthFormat {
		DEPTHSTENCIL_NONE, DEPTH_16, STENCIL_8, DEPTHSTENCIL_24_8;
	}

	public static class FrameBufferBuilder extends GLFrameBufferBuilder<FrameBuffer> {
		public FrameBufferBuilder(int width, int height) {
			super(width, height);
		}

		@Override
		public FrameBuffer build() {
			return new FrameBuffer();
		}
	}

	protected final static int GL_DEPTH_COMPONENT = 0x1902;

	protected final static int GL_DEPTH_STENCIL_OES = 0x84F9;

	protected final static int GL_UNSIGNED_SHORT = 0x1403;

	protected final static int GL_UNSIGNED_INT = 0x1405;

	protected final static int GL_UNSIGNED_INT_24_8_OES = 0x84FA;

	protected final static int GL_DEPTH_COMPONENT16 = 0x81A5;

	protected final static int GL_DEPTH_COMPONENT32_OES = 0x81A7;

	protected final static int GL_DEPTH24_STENCIL8_OES = 0x88F0;

	protected TArray<LTexture> textureAttachments = new TArray<LTexture>();

	protected static int defaultFramebufferHandle;

	protected static boolean defaultFramebufferHandleInitialized = false;

	protected int framebufferHandle;

	protected int depthbufferHandle;

	protected int stencilbufferHandle;

	protected int depthStencilPackedBufferHandle;

	protected boolean hasDepthStencilPackedBuffer;

	protected GLFrameBufferBuilder<? extends GLFrameBuffer> bufferBuilder;

	GLFrameBuffer() {
	}

	protected GLFrameBuffer(GLFrameBufferBuilder<? extends GLFrameBuffer> bufferBuilder) {
		this.bufferBuilder = bufferBuilder;
		build();
	}

	public LTexture getColorBufferTexture() {
		return textureAttachments.first();
	}

	public TArray<LTexture> getTextureAttachments() {
		return textureAttachments;
	}

	public void clear(float r, float g, float b, float a) {
		this.clear(r, g, b, a, FrameBufferDepthFormat.DEPTHSTENCIL_NONE);
	}

	public void clear(float r, float g, float b, float a, FrameBufferDepthFormat depthStencilFormat) {
		GL20 gl = LSystem.base().graphics().gl;
		gl.glClearColor(r, g, b, a);
		int flag = GL20.GL_COLOR_BUFFER_BIT;
		switch (depthStencilFormat) {
		case DEPTHSTENCIL_NONE:
			flag |= GL20.GL_DEPTH_BUFFER_BIT;
			return;
		case DEPTH_16:
			flag |= GL20.GL_DEPTH_BUFFER_BIT;
			break;
		case STENCIL_8:
			flag |= GL20.GL_STENCIL_BUFFER_BIT;
			break;
		case DEPTHSTENCIL_24_8:
			flag |= GL20.GL_DEPTH_BUFFER_BIT;
			flag |= GL20.GL_STENCIL_BUFFER_BIT;
			break;
		}
		gl.glClear(flag);
	}

	public LTexture getTextureData() {
		return getTextureData(true, true);
	}

	public LTexture getTextureData(boolean flip, boolean alpha) {
		return getImageData(0, flip, alpha).texture();
	}

	public Image getImageData(int index, boolean flip, boolean alpha) {
		GL20 gl = LSystem.base().graphics().gl;
		final int nfb = gl.glGenFramebuffer();
		if (nfb == 0) {
			throw new LSysException("Failed to gen framebuffer: " + gl.glGetError());
		}
		LTexture texture = getTextureAttachments().get(index);
		gl.glBindFramebuffer(GL_FRAMEBUFFER, nfb);
		gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture.getID(), 0);
		boolean canRead = GLUtils.isFrameBufferCompleted(gl);
		if (!canRead) {
			return null;
		}
		Image image = GLUtils.getFrameBuffeImage(gl, 0, 0, getWidth(), getHeight(), flip, alpha);
		gl.glBindFramebuffer(GL_FRAMEBUFFER, defaultFramebufferHandle);
		gl.glDeleteFramebuffer(nfb);
		return image;
	}

	protected void build() {
		GL20 gl = LSystem.base().graphics().gl;

		if (!defaultFramebufferHandleInitialized) {
			defaultFramebufferHandleInitialized = true;
			if (LSystem.base().type() == LGame.Type.IOS) {
				IntBuffer intbuf = ByteBuffer.allocateDirect(16 * Integer.SIZE / 8).order(ByteOrder.nativeOrder())
						.asIntBuffer();
				gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, intbuf);
				defaultFramebufferHandle = intbuf.get(0);
			} else {
				defaultFramebufferHandle = 0;
			}
		}

		framebufferHandle = gl.glGenFramebuffer();
		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);

		int width = bufferBuilder.width;
		int height = bufferBuilder.height;

		if (bufferBuilder.hasDepthRenderBuffer) {
			depthbufferHandle = gl.glGenRenderbuffer();
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, depthbufferHandle);
			gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, bufferBuilder.depthRenderBufferSpec.internalFormat, width,
					height);
		}

		if (bufferBuilder.hasStencilRenderBuffer) {
			stencilbufferHandle = gl.glGenRenderbuffer();
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, stencilbufferHandle);
			gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, bufferBuilder.stencilRenderBufferSpec.internalFormat, width,
					height);
		}

		LTexture texture = createTexture(
				(FrameBufferTextureAttachmentSpec) bufferBuilder.textureAttachmentSpecs.first());
		textureAttachments.add(texture);
		GLUtils.bindTexture(gl, texture.getID());

		attachFrameBufferColorTexture(textureAttachments.first());

		if (bufferBuilder.hasDepthRenderBuffer) {
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL20.GL_RENDERBUFFER,
					depthbufferHandle);
		}

		if (bufferBuilder.hasStencilRenderBuffer) {
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_STENCIL_ATTACHMENT, GL20.GL_RENDERBUFFER,
					stencilbufferHandle);
		}

		gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0);
		for (LTexture tex : textureAttachments) {
			GLUtils.bindTexture(gl, tex);
		}

		int result = gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER);

		if (result == GL20.GL_FRAMEBUFFER_UNSUPPORTED && bufferBuilder.hasDepthRenderBuffer
				&& bufferBuilder.hasStencilRenderBuffer) {
			if (bufferBuilder.hasDepthRenderBuffer) {
				gl.glDeleteRenderbuffer(depthbufferHandle);
				depthbufferHandle = 0;
			}
			if (bufferBuilder.hasStencilRenderBuffer) {
				gl.glDeleteRenderbuffer(stencilbufferHandle);
				stencilbufferHandle = 0;
			}

			depthStencilPackedBufferHandle = gl.glGenRenderbuffer();
			hasDepthStencilPackedBuffer = true;
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, depthStencilPackedBufferHandle);
			gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, GL_DEPTH24_STENCIL8_OES, width, height);
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0);

			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL20.GL_RENDERBUFFER,
					depthStencilPackedBufferHandle);
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_STENCIL_ATTACHMENT, GL20.GL_RENDERBUFFER,
					depthStencilPackedBufferHandle);
			result = gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER);
		}

		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle);

		if (result != GL20.GL_FRAMEBUFFER_COMPLETE) {
			for (LTexture tex : textureAttachments) {
				disposeColorTexture(tex);
			}

			if (hasDepthStencilPackedBuffer) {
				gl.glDeleteBuffer(depthStencilPackedBufferHandle);
			} else {
				if (bufferBuilder.hasDepthRenderBuffer)
					gl.glDeleteRenderbuffer(depthbufferHandle);
				if (bufferBuilder.hasStencilRenderBuffer)
					gl.glDeleteRenderbuffer(stencilbufferHandle);
			}

			gl.glDeleteFramebuffer(framebufferHandle);

			if (result == GL20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT)
				throw new LSysException("Frame buffer couldn't be constructed: incomplete attachment");
			if (result == GL20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS)
				throw new LSysException("Frame buffer couldn't be constructed: incomplete dimensions");
			if (result == GL20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT)
				throw new LSysException("Frame buffer couldn't be constructed: missing attachment");
			if (result == GL20.GL_FRAMEBUFFER_UNSUPPORTED)
				throw new LSysException("Frame buffer couldn't be constructed: unsupported combination of formats");
			throw new LSysException("Frame buffer couldn't be constructed: unknown error " + result);
		}

		addManagedFrameBuffer(this);
	}

	@Override
	public void close() {
		GL20 gl = LSystem.base().graphics().gl;

		for (LTexture texture : textureAttachments) {
			disposeColorTexture(texture);
		}

		if (hasDepthStencilPackedBuffer) {
			gl.glDeleteRenderbuffer(depthStencilPackedBufferHandle);
		} else {
			if (bufferBuilder.hasDepthRenderBuffer)
				gl.glDeleteRenderbuffer(depthbufferHandle);
			if (bufferBuilder.hasStencilRenderBuffer)
				gl.glDeleteRenderbuffer(stencilbufferHandle);
		}

		gl.glDeleteFramebuffer(framebufferHandle);

		LSystem.removeFrameBuffer(this);
	}

	public void bind(GL20 gl) {
		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);
	}

	public void unbind(GL20 gl) {
		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle);
	}

	public void bind() {
		bind(LSystem.base().graphics().gl);
	}

	public void unbind() {
		unbind(LSystem.base().graphics().gl);
	}

	public void begin() {
		bind();
		setFrameBufferViewport();
	}

	protected void setFrameBufferViewport() {
		LSystem.base().graphics().gl.glViewport(0, 0, bufferBuilder.width, bufferBuilder.height);
	}

	public void end() {
		end(0, 0, bufferBuilder.width, bufferBuilder.height);
	}

	public void end(int x, int y, int width, int height) {
		unbind();
		LSystem.base().graphics().gl.glViewport(x, y, width, height);
	}

	public int getFramebufferHandle() {
		return framebufferHandle;
	}

	public int getDepthBufferHandle() {
		return depthbufferHandle;
	}

	public int getStencilBufferHandle() {
		return stencilbufferHandle;
	}

	protected int getDepthStencilPackedBuffer() {
		return depthStencilPackedBufferHandle;
	}

	public int getHeight() {
		return bufferBuilder.height;
	}

	public int getWidth() {
		return bufferBuilder.width;
	}

	private void addManagedFrameBuffer(GLFrameBuffer frameBuffer) {
		LSystem.addFrameBuffer(frameBuffer);
	}

	public static void invalidate(LGame game) {
		if (game.graphics().gl == null) {
			return;
		}
		TArray<GLFrameBuffer> bufferArray = game.getFrameBufferAll();
		if (bufferArray == null) {
			return;
		}
		for (int i = 0; i < bufferArray.size; i++) {
			bufferArray.get(i).build();
		}
	}

	public void clearAllFrameBuffers() {
		LSystem.clearFramebuffer();
	}

	protected static class FrameBufferTextureAttachmentSpec {
		int internalFormat, format, type;
		boolean isFloat, isGpuOnly;
		boolean isDepth;
		boolean isStencil;

		public FrameBufferTextureAttachmentSpec(int internalformat, int format, int type) {
			this.internalFormat = internalformat;
			this.format = format;
			this.type = type;
		}

		public boolean isColorTexture() {
			return !isDepth && !isStencil;
		}
	}

	protected static class FrameBufferRenderBufferAttachmentSpec {
		int internalFormat;

		public FrameBufferRenderBufferAttachmentSpec(int internalFormat) {
			this.internalFormat = internalFormat;
		}
	}

	protected static abstract class GLFrameBufferBuilder<U extends GLFrameBuffer> {
		protected int width, height;

		protected TArray<FrameBufferTextureAttachmentSpec> textureAttachmentSpecs = new TArray<FrameBufferTextureAttachmentSpec>();

		protected FrameBufferRenderBufferAttachmentSpec stencilRenderBufferSpec;
		protected FrameBufferRenderBufferAttachmentSpec depthRenderBufferSpec;
		protected FrameBufferRenderBufferAttachmentSpec packedStencilDepthRenderBufferSpec;

		protected boolean hasStencilRenderBuffer;
		protected boolean hasDepthRenderBuffer;

		public GLFrameBufferBuilder(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public GLFrameBufferBuilder<U> addColorTextureAttachment(int internalFormat, int format, int type) {
			textureAttachmentSpecs.add(new FrameBufferTextureAttachmentSpec(internalFormat, format, type));
			return this;
		}

		public GLFrameBufferBuilder<U> addFloatAttachment(int internalFormat, int format, int type, boolean gpuOnly) {
			FrameBufferTextureAttachmentSpec spec = new FrameBufferTextureAttachmentSpec(internalFormat, format, type);
			spec.isFloat = true;
			spec.isGpuOnly = gpuOnly;
			textureAttachmentSpecs.add(spec);
			return this;
		}

		public GLFrameBufferBuilder<U> addBasicColorTextureAttachment(int glFormat, int glType) {
			return addColorTextureAttachment(glFormat, glFormat, glType);
		}

		public GLFrameBufferBuilder<U> addDepthTextureAttachment(int internalFormat, int type) {
			FrameBufferTextureAttachmentSpec spec = new FrameBufferTextureAttachmentSpec(internalFormat,
					GL20.GL_DEPTH_COMPONENT, type);
			spec.isDepth = true;
			textureAttachmentSpecs.add(spec);
			return this;
		}

		public GLFrameBufferBuilder<U> addStencilTextureAttachment(int internalFormat, int type) {
			FrameBufferTextureAttachmentSpec spec = new FrameBufferTextureAttachmentSpec(internalFormat,
					GL20.GL_STENCIL_ATTACHMENT, type);
			spec.isStencil = true;
			textureAttachmentSpecs.add(spec);
			return this;
		}

		public GLFrameBufferBuilder<U> addDepthRenderBuffer(int internalFormat) {
			depthRenderBufferSpec = new FrameBufferRenderBufferAttachmentSpec(internalFormat);
			hasDepthRenderBuffer = true;
			return this;
		}

		public GLFrameBufferBuilder<U> addStencilRenderBuffer(int internalFormat) {
			stencilRenderBufferSpec = new FrameBufferRenderBufferAttachmentSpec(internalFormat);
			hasStencilRenderBuffer = true;
			return this;
		}

		public GLFrameBufferBuilder<U> addBasicDepthRenderBuffer() {
			return addDepthRenderBuffer(GL20.GL_DEPTH_COMPONENT16);
		}

		public GLFrameBufferBuilder<U> addBasicStencilRenderBuffer() {
			return addStencilRenderBuffer(GL20.GL_STENCIL_INDEX8);
		}

		public abstract U build();
	}

	protected abstract LTexture createTexture(FrameBufferTextureAttachmentSpec attachmentSpec);

	protected abstract void disposeColorTexture(LTexture colorTexture);

	protected abstract void attachFrameBufferColorTexture(LTexture texture);

}
