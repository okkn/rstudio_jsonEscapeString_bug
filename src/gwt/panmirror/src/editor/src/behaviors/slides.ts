/*
 * slides.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { Transaction, EditorState } from "prosemirror-state";
import { setTextSelection } from "prosemirror-utils";

import { ExtensionContext } from "../api/extension";
import { kPresentationDocType } from "../api/format";
import { canInsertNode, editingRootNode } from "../api/node";
import { ProsemirrorCommand, EditorCommandId } from "../api/command";
import { OmniInsertGroup } from "../api/omni_insert";

const extension = (context: ExtensionContext) => {

  const { ui, format, pandocExtensions } = context;

  if (!format.docTypes.includes(kPresentationDocType)) {
    return null;
  }

  return {
    commands: () => {
      const cmds: ProsemirrorCommand[] = [
        new ProsemirrorCommand(EditorCommandId.InsertSlidePause, [], insertSlidePause, {
          name: ui.context.translateText('Slide Pause'),
          description: ui.context.translateText('Pause after content'),
          group: OmniInsertGroup.Content,
          priority: 2,
          image: () => ui.images.omni_insert?.generic!,
        })
      ];

      if (pandocExtensions.fenced_divs) {
        cmds.push(new ProsemirrorCommand(EditorCommandId.InsertSlideNotes, [], insertSlideNotes, {
          name: ui.context.translateText('Slide Notes'),
          description: ui.context.translateText('Slide speaker notes'),
          group: OmniInsertGroup.Content,
          priority: 2,
          image: () => ui.images.omni_insert?.generic!,
        }));
      }

      return cmds;
    }

  };
};

export function insertSlidePause(state: EditorState, dispatch?: (tr: Transaction) => void) {
  const schema = state.schema;
  if (!canInsertNode(state, schema.nodes.paragraph)) {
    return false;
  }
  if (dispatch) {
    const tr = state.tr;
    tr.replaceSelectionWith(schema.nodes.paragraph.createAndFill({}, schema.text('. . .')));
    setTextSelection(tr.selection.from - 1, -1)(tr);
    tr.replaceSelectionWith(schema.nodes.paragraph.create());
    setTextSelection(tr.selection.from - 1, -1)(tr);
    dispatch(tr);
  }
  return true;
}

export function insertSlideNotes(state: EditorState, dispatch?: (tr: Transaction) => void) {
  const schema = state.schema;
  if (!canInsertNode(state, schema.nodes.div)) {
    return false;
  }
  if (dispatch) {
    const tr = state.tr;
    tr.replaceSelectionWith(schema.nodes.div.createAndFill({ classes: ['notes']}, schema.nodes.paragraph.create()));
    setTextSelection(state.selection.from + 1, 1)(tr);
    dispatch(tr);
  }
  return true;
}

export default extension;