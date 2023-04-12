import * as React from 'react'
import '@testing-library/jest-dom'
import {render, screen} from '@testing-library/react'
import {testEmail1} from "../../setupTests";
import {HtmlContentTabPanel} from "./html-content-tab-panel";

describe('HtmlContentTabPanel', () => {
    it('render html content tab panel component with simple html', () => {
        render(<HtmlContentTabPanel activeContentType={"html"} email={testEmail1} data={"<p>test1</p><p>test2</p>"} />);

        expect(screen.getByText("test1")).toBeInTheDocument()
        expect(screen.getByText("test2")).toBeInTheDocument()
    })
    it('render html content tab panel with inline images with single quotes', () => {
        const email = {...testEmail1}
        email.inlineImages = [
            {contentId: "img1", data: "FIRST_IMAGE", contentType: "image/png"},
            {contentId: "img2", data: "SECOND_IMAGE", contentType: "image/png"}
        ]

        render(<HtmlContentTabPanel activeContentType={"html"} email={email} data={"<p>test1</p><p>test2</p><img alt='img1' src='cid:img1' /><img alt='img2' src='cid:img2' />"} />);

        expect(screen.getByText("test1")).toBeInTheDocument()
        expect(screen.getByText("test2")).toBeInTheDocument()
        expect(screen.getByAltText("img1")).toHaveAttribute("src", "data:image/png;base64,FIRST_IMAGE")
        expect(screen.getByAltText("img2")).toHaveAttribute("src", "data:image/png;base64,SECOND_IMAGE")
    })
    it('render html content tab panel with inline images with double quotes', () => {
        const email = {...testEmail1}
        email.inlineImages = [
            {contentId: "img1", data: "FIRST_IMAGE", contentType: "image/png"},
            {contentId: "img2", data: "SECOND_IMAGE", contentType: "image/png"}
        ]

        render(<HtmlContentTabPanel activeContentType={"html"} email={email} data={"<p>test1</p><p>test2</p><img alt=\"img1\" src=\"cid:img1\" /><img alt=\"img2\" src=\"cid:img2\" />"} />);

        expect(screen.getByText("test1")).toBeInTheDocument()
        expect(screen.getByText("test2")).toBeInTheDocument()
        expect(screen.getByAltText("img1")).toHaveAttribute("src", "data:image/png;base64,FIRST_IMAGE")
        expect(screen.getByAltText("img2")).toHaveAttribute("src", "data:image/png;base64,SECOND_IMAGE")
    })
    it('render html content tab panel with inline images as is when image not found', () => {
        render(<HtmlContentTabPanel activeContentType={"html"} email={testEmail1} data={"<p>test1</p><p>test2</p><img alt=\"img1\" src=\"cid:img1\" /><img alt=\"img2\" src=\"cid:img2\" />"} />);

        expect(screen.getByText("test1")).toBeInTheDocument()
        expect(screen.getByText("test2")).toBeInTheDocument()
        expect(screen.getByAltText("img1")).toHaveAttribute("src", "cid:img1")
        expect(screen.getByAltText("img2")).toHaveAttribute("src", "cid:img2")
    })
})